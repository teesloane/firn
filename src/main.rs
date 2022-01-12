use std::env;
use std::path::PathBuf;
pub mod config;
pub mod errors;
pub mod front_matter;
pub mod html;
pub mod new_site;
pub mod org;
pub mod serve;
pub mod templates;
pub mod user_config;
pub mod util;
use anyhow::Result;

extern crate clap;
use clap::{App, AppSettings, Arg};

fn main() -> Result<()> {
    let matches = App::new("Firn")
        .setting(AppSettings::ArgRequiredElseHelp)
        .version("0.15")
        .author("The Ice Shelf")
        .about("Org Mode Static Site Generator")
        .arg(
            Arg::new("directory")
                .short('d')
                .long("dir")
                .help("filesystem path to operate new/build/serve from")
                .global(true)
                .takes_value(true),
        )
        .arg(
            Arg::new("verbose")
                .short('v')
                .long("verbose")
                .multiple_occurrences(true)
                .global(true)
                .help("Sets the level of verbosity"),
        )
        .subcommand(
            App::new("new")
                .about("Scaffolds files & folders needed to start a new site."),
        )
        .subcommand(
            App::new("build")
                .about("Build a static site in a directory with org files."),
        )
        .subcommand(
            App::new("serve")
                .about("Runs a development server for processed org files.")
                .arg(
                    Arg::new("port")
                        .short('p')
                        .help("set the port for the development server to run on.")
                        .long("port")
                        .takes_value(true),
                ),
        )
        .get_matches();

    let get_dir = || -> PathBuf {
        let dir: PathBuf;
        if matches.is_present("directory") {
            let dir_str = matches
                .value_of("directory")
                .expect("Failed to get directory value when running 'firn new'");
            dir = PathBuf::from(dir_str);
        } else {
            dir = env::current_dir().expect("Failed to get cwd");
        }
        dir
    };

    let get_verbosity = || -> i8 {
        match matches.occurrences_of("verbose") {
            0 => 0,
            1 => 1,
            _ => 2,
        }
    };
    // Command: new ------------------------------------------------------------
    if let Some(_matches) = matches.subcommand_matches("new") {
        let dir = get_dir();
        new_site::init(dir);
    }
    // Command: build ----------------------------------------------------------
    if let Some(_matches) = matches.subcommand_matches("build") {
        let dir = get_dir();
        let mut config = unwrap_config(dir, get_verbosity());
        config.build(true)?;
    }

    if let Some(serve_match) = matches.subcommand_matches("serve") {
        let port = serve_match.value_of("port").unwrap_or("8080");
        let dir = get_dir();
        let mut config = unwrap_config(dir, get_verbosity());
        config.setup_for_serve(port);
        config.build(true)?;
        serve::start_server(&mut config);
    }
    Ok(())
}

fn unwrap_config(cwd_as_path: PathBuf, verbosity: i8) -> config::Config<'static> {
    let config = match config::Config::new(cwd_as_path, verbosity) {
        Ok(cfg) => cfg,
        Err(_e) => {
            println!("{:?}", _e);
            util::exit();
        }
    };
    config
}
