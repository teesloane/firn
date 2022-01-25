use notify::DebouncedEvent::*;
use notify::{watcher, RecursiveMode, Watcher};
use std::net::{IpAddr, SocketAddr, Ipv4Addr};
use std::path::Path;
use std::{io, path::PathBuf, thread};
// use std::io;
use std::sync::mpsc::channel;
use std::{io::Write, time::Duration};

use crate::config::Config;

// possible live reload example
// https://github.com/seanmonstar/warp/blob/master/examples/autoreload.rs
pub fn start_server(cfg: &mut Config) {
    let dir_out = cfg.dir_site_out.clone();
    let port = cfg.serve_port;

    thread::spawn(move || {
        let rt = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .expect("Could not build tokio runtime");

        rt.block_on(async {
            println!("Starting server at http://localhost:{}", port);
            let socket = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)), port);
            warp::serve(warp::fs::dir(dir_out))
                .run(socket)
                .await;
        });
    });
    watch_stuff(cfg);
}

fn watch_stuff(cfg: &mut Config) {
    let paths_to_watch = vec![
        (cfg.dir_sass.clone(), RecursiveMode::Recursive),
        (cfg.dir_templates.clone(), RecursiveMode::Recursive),
        (cfg.dir_static_src.clone(), RecursiveMode::Recursive),
        (cfg.dir_data_files_src.clone(), RecursiveMode::Recursive),
        (cfg.dir_firn.clone(), RecursiveMode::NonRecursive), // for finding config.yaml
        // NOTE:
        // We may want to switch this to NonRecursive because firn works in a
        // way that it operates on a folder already full of org files... if it
        // we watch recursively (say you have blog/foo.org), when that changes
        // it will recompile the site, and then the watcher will look at every
        // compiled file in _firn/_site, and try and handle those changes.
        //
        // This works, but... it bugs me.
        // If notify-rs ever enables filtered paths, we could switch to recursive.
        (cfg.dir_source.clone(), RecursiveMode::Recursive),
    ];

    // Create a channel to receive the events.
    let (tx, rx) = channel();

    // Create a watcher object, delivering debounced events.
    // The notification back-end is selected based on the platform.
    let mut watcher = watcher(tx, Duration::from_secs(1)).unwrap();

    // Add a path to be watched. All files and directories at that path and
    // below will be monitored for changes.
    for (path, recur_mode) in paths_to_watch {
        if path.exists() {
            watcher.watch(path, recur_mode).unwrap();
        }
    }

    loop {
        match rx.recv() {
            Ok(event) => match event {
                Rename(_, path) | Create(path) | Write(path) | Remove(path) | Chmod(path) => {
                    let changeset = detect_change_kind(&cfg.dir_source, path);
                    handle_change_kind(changeset, cfg)
                }
                _ => {}
            },
            Err(e) => println!("watch error: {:?}", e),
        }
    }
}

#[derive(Debug)]
pub enum ChangeKind {
    Sass,
    Layouts,
    OrgFile,
    DataFile,
    StaticFile,
    Config,
    Unknown,
}

fn detect_change_kind(dir_source: &Path, path: PathBuf) -> (PathBuf, ChangeKind) {
    let starting_folder = dir_source.file_stem().unwrap();
    let mut changed_path = PathBuf::from(starting_folder);
    changed_path.push(path.strip_prefix(dir_source).unwrap_or(&path));
    let combined_path = |s: &str| PathBuf::from(starting_folder).join("_firn").join(s);

    let change_kind = if changed_path.starts_with(combined_path("layouts")) {
        ChangeKind::Layouts
    } else if changed_path.starts_with(combined_path("static")) {
        ChangeKind::StaticFile
    } else if changed_path.starts_with(combined_path("sass")) {
        ChangeKind::Sass
    } else if changed_path == combined_path("config.yaml") {
        ChangeKind::Config
    // this should be at the end of the block!
    // detects if it's an org file that has changed
    } else if let Some(_parent) = changed_path.parent() {
        if let Some(extension) = changed_path.extension() {
            if  extension == "org" {
                ChangeKind::OrgFile
            } else {
                ChangeKind::Unknown
            }
        } else {
            ChangeKind::Unknown
        }
    } else {
        ChangeKind::Unknown
    };
    (changed_path, change_kind)
}

/// handle_change_kind responds to different file/folders that change, and rebuilds
/// the respective things when matched.
/// All of these are very naive right now: there is no caching, there is no smart compilation.
/// NOTE: it might not be worth it to improve things here until reload becomes unbearable
/// as so much depends on global values being collecting everytime Firn runs.
fn handle_change_kind((_partial_path, change): (PathBuf, ChangeKind), cfg: &mut Config) {
    let flush = || io::stdout().flush().expect("Could not flush stdout");
    let mut rebuild = |thing: &str| {
        print!("Rebuilding {}...", thing);
        flush();
        match thing {
            "site" => cfg.rebuild(false).unwrap(),
            "sass" => cfg.compile_scss().unwrap(),
            "data" => cfg.cp_data(),
            "static" => {
                cfg.cp_static();
                // we recompile scss incase the static folder re-copy overwrites prev scss.
                cfg.compile_scss().unwrap();
            },
            "layouts" => {
                cfg.tera.full_reload().expect("Failed to reload layout change.");
                cfg.rebuild(false).expect("Failed to rebuild.");
            }
            "config" => {
                cfg.reload_config().expect("Failed to reload config.");
            }
            _ => (),
        }
        // cfg.build(false).unwrap();
        println!("\rRebuilding {}...done", thing);
        flush();
    };

    match change {
        ChangeKind::Sass => rebuild("sass"),
        ChangeKind::Layouts => rebuild("layouts"),
        ChangeKind::OrgFile => rebuild("site"),
        ChangeKind::DataFile => rebuild("data"),
        ChangeKind::StaticFile => rebuild("static"),
        ChangeKind::Config => rebuild("config"),
        ChangeKind::Unknown => {}
    }
}
