use crate::{
    errors::{FirnError, FirnErrorType},
    org,
    org::OrgMetadata,
    templates::{self},
    templates::{
        data,
        links::{LinkData, LinkMeta},
    },
    user_config::UserConfig,
    util,
};

use anyhow::{Context, Result};
use glob::glob;
use rayon::prelude::*;
use sass_rs::{compile_file, Options, OutputStyle};
use std::fs;
use std::path::PathBuf;
use std::process::Command;
use std::{collections::HashMap, fs::create_dir_all};
use tera;

pub struct Config<'a> {
    pub dir_source: PathBuf,
    pub dir_firn: PathBuf,
    pub dir_templates: PathBuf,
    pub dir_site_out: PathBuf,
    pub dir_static_src: PathBuf,
    pub dir_static_dest: PathBuf,
    pub dir_data_files_src: PathBuf,
    pub dir_data_files_dest: PathBuf,
    pub dir_tags: PathBuf,
    pub dir_sass: PathBuf,
    pub serve_port: u16,
    pub paths_org_files: Vec<PathBuf>,
    pub org_files: Vec<org::OrgFile<'a>>,
    pub global_tags: Vec<OrgMetadata<'a>>,
    pub global_sitemap: HashMap<String, OrgMetadata<'a>>,
    pub global_links: Vec<OrgMetadata<'a>>,
    pub global_logbook: Vec<OrgMetadata<'a>>,
    pub global_attachments: Vec<String>,
    pub tera: tera::Tera,
    pub verbosity: i8,
    // Data specifically for templates / user interaction:
    pub user_config: UserConfig,
    pub sitemap: Vec<LinkData>,
    pub sitemap_mru: Vec<LinkData>,
    pub sitemap_mrp: Vec<LinkData>,
    pub tag_page: PathBuf,
    pub tags_map: HashMap<String, Vec<OrgMetadata<'a>>>,
    pub tags_list: Vec<LinkData>,
    pub base_url: String,
}

/// Builds common paths for the config object.
fn build_paths(cwd: &PathBuf) -> (PathBuf, PathBuf, PathBuf, PathBuf) {
    let dir_firn = PathBuf::new().join(&cwd).join("_firn");
    let dir_templates = dir_firn.join("layouts");
    let dir_site_out = dir_firn.join("_site");
    let config_file = dir_firn.join("config.yaml");
    (dir_firn, dir_templates, dir_site_out, config_file)
}

impl<'a> Config<'a> {
    pub fn new(cwd: PathBuf, verbosity: i8) -> Result<Config<'a>, anyhow::Error> {
        let (dir_firn, dir_templates, dir_site_out, config_file) = build_paths(&cwd);
        Config::check_site_exists(&dir_firn);

        let user_config_str = std::fs::read_to_string(&config_file).with_context(|| {
            "\nError: Failed to load user config file. Does _firn/config.yaml exist?".to_string()
        })?;
        let user_config: UserConfig = serde_yaml::from_str(&user_config_str).with_context(|| {
            "\nError: Failed to parse user config file. _firn/config.yaml does not conform to Firn's requirements.".to_string()
        })?;
        user_config.validate();
        let dir_data_files_src = cwd.join(&user_config.site.data_directory);
        let dir_data_files_dest = dir_firn
            .join("_site/")
            .join(&user_config.site.data_directory);
        let dir_tags = dir_site_out.join(user_config.get_tag_url());
        let tag_page = dir_firn.join("[tags].html");

        Ok(Config {
            dir_source: cwd,
            global_attachments: Vec::new(),
            tera: templates::tera::load_templates(&dir_templates.clone()),
            base_url: user_config.site.url.clone(),
            dir_static_src: dir_firn.join("static"),
            dir_static_dest: dir_firn.join("_site/static"),
            dir_sass: dir_firn.join("sass"),
            dir_firn,
            dir_templates,
            dir_site_out,
            dir_data_files_src,
            dir_data_files_dest,
            dir_tags,
            tag_page,
            tags_list: Vec::new(),
            tags_map: HashMap::new(),
            serve_port: 8080,
            sitemap: Vec::new(),
            sitemap_mru: Vec::new(),
            sitemap_mrp: Vec::new(),
            paths_org_files: Vec::new(),
            org_files: Vec::new(),
            verbosity,
            // maybe these should maybe be prefixed with "raw", since we don't use that data except to munge it
            global_tags: Vec::new(),
            global_links: Vec::new(),
            global_sitemap: HashMap::new(),
            global_logbook: Vec::new(),
            user_config,
        })
    }

    /// load_firn_files - Load the files from _firn into memory:
    /// org files, and user config (TODO).
    fn load_firn_files(&mut self) {
        self.paths_org_files = util::load_files(&self.dir_source, "**/*.org");
    }

    pub fn reload_config(&mut self) -> anyhow::Result<()> {
        let config_file = self.dir_firn.join("config.yaml");
        let user_config_str = std::fs::read_to_string(&config_file).with_context(|| {
            "\nError: Failed to load user config file. Does _firn/config.yaml exist?".to_string()
        })?;
        let user_config: UserConfig = serde_yaml::from_str(&user_config_str).with_context(|| {
            "\nError: Failed to parse user config file. _firn/config.yaml does not conform to Firn's requirements.".to_string()
        })?;
        user_config.validate();
        self.user_config = user_config;
        self.tera.full_reload().unwrap();
        self.rebuild(false).unwrap();
        Ok(())
    }

    /// For now we shell out to cp on unix because I don't want to figure this out in rust
    /// and windows support for Firn doesn't exist in the clojure version anyway.
    /// copies data and static folder to their respective destinations
    pub fn cp_data(&mut self) {
        // for some reason I need to create _site/dest so cp works...
        create_dir_all(self.dir_data_files_dest.clone()).unwrap();
        Command::new("cp")
            .arg("-n")
            .arg("-r")
            .arg(self.dir_data_files_src.display().to_string())
            .arg(self.dir_site_out.display().to_string())
            .output()
            .expect("Internal error: failed to copy data directory to _site.");
    }

    pub fn cp_static(&mut self) {
        create_dir_all(self.dir_static_dest.clone()).unwrap();
        Command::new("cp")
            .arg("-r")
            .arg(self.dir_static_src.display().to_string())
            .arg(self.dir_site_out.display().to_string())
            .output()
            .expect("CP command failed to move static folder to _site.");
    }

    /// compiles scss found in sass folder and moves it into static/css
    pub fn compile_scss(&self) -> anyhow::Result<()> {
        // ripped from: https://github.com/getzola/zola/blob/3dcc080f9d3558e9f811b442ebe09699f363f930/components/site/src/sass.rs
        // get all non partial scss
        let glob_string = format!(
            "{}/**/*.{}",
            self.dir_sass.display(),
            self.user_config.site.sass
        );
        let files = glob(&glob_string)
            .expect("Invalid glob for sass")
            .filter_map(|e| e.ok())
            .filter(|entry| {
                !entry
                    .as_path()
                    .iter()
                    .last()
                    .map(|c| c.to_string_lossy().starts_with('_'))
                    .unwrap_or(true)
            })
            .collect::<Vec<_>>();
        let options = Options {
            output_style: OutputStyle::Compressed,
            ..Default::default()
        };
        for file in files {
            let out_path = file.strip_prefix(&self.dir_sass)?;
            let css_output_path = self
                .dir_static_dest
                .join("css")
                .join(out_path)
                .with_extension("css");
            // all parent dirs we need to recreate in static/css/<here>
            if out_path.parent().is_some() {
                create_dir_all(&css_output_path.parent().unwrap())?;
            }

            // NOTE: at the moment we don't panic or abort on failed config
            match compile_file(self.dir_sass.join(&file), options.clone()) {
                Ok(res) => {
                    fs::write(css_output_path, &res).context("Failed to write css to file")?;
                }
                Err(s) => {
                    println!("\n{}\n", s);
                }
            };
        }
        Ok(())
    }

    /// parse_files creates our org files and attach to the config object.
    /// uses rayon to speed up iteration.
    fn parse_files(&mut self) {
        let paths_org_files = self.paths_org_files.clone();
        let org_files: Vec<_> = paths_org_files
            .par_iter()
            .map(|f| {
                let file_path = f.clone();
                let read_file = fs::read_to_string(f).expect("Unable to read file");

                org::OrgFile::new(read_file, self, file_path)
            })
            .collect();

        self.org_files = org_files;
    }

    pub fn clone_baseurl(&self) -> String {
        self.user_config.site.url.clone()
    }

    /// collect_global_data loops through all org files and aggregates all
    /// links, logs, tags, into one place, then munges that data into
    /// more user friendly maps and  vectors for templates to consume.
    fn collect_global_data(&mut self) {
        // first, we iterate through all the org files and get the metadata for each.
        for f in &self.org_files {
            self.global_attachments.append(&mut f.attachments.clone());

            if !f.is_private(&self.user_config.site.ignored_directories, &self.dir_source) {
                self.global_links.append(&mut f.links.clone());
                self.global_logbook.append(&mut f.logbook.clone());
                self.global_tags.append(&mut f.tags.clone());
                if f.front_matter.firn_sitemap {
                    self.global_sitemap
                        .insert(f.front_matter.get_title(), f.sitemap_data.clone());
                }
            }
        }
        // println!("self.attachments {:?}", self.global_attachments);

        // after we do that, munge some of that data (tags, mostly), and re-attach it to self.
        // Collecting tagged files into a map:
        let mut x: HashMap<String, Vec<OrgMetadata<'a>>> = HashMap::new();
        for tag in &self.global_tags {
            match &tag.entity {
                org::OrgMetadataType::Tag(tag_name, tag_type) => match tag_type {
                    org::OrgTagType::FirnTag => {
                        if self.user_config.tags.firn {
                            x.entry(tag_name.to_string().to_lowercase())
                                .and_modify(|e| e.push(tag.to_owned()))
                                .or_insert_with(|| vec![tag.to_owned()]);
                        }
                    }
                    org::OrgTagType::OrgTag => {
                        if self.user_config.tags.org {
                            x.entry(tag_name.to_string().to_lowercase())
                                .and_modify(|e| e.push(tag.to_owned()))
                                .or_insert_with(|| vec![tag.to_owned()]);
                        }
                    }
                },
                _ => (),
            }
        }
        self.tags_map = x;

        // -- Tags: list of LinkData for templates --

        let mut out: Vec<LinkData> = Vec::new();
        for (tag_name, v) in &self.tags_map {
            let tag_url = format!(
                "{}/{}{}.html",
                self.user_config.site.url, self.user_config.tags.url, tag_name
            );
            let x = LinkData::new(
                tag_url,
                tag_name.to_string(),
                LinkMeta::Tag{count: v.len()},
                None
            );
            out.push(x);
        }
        out.sort_by_key(|ld| ld.file.clone());
        self.tags_list = out;

        // -- Sitemap --
        let mut out: Vec<LinkData> = Vec::new();
        for (_k, v) in &self.global_sitemap {
            match &v.entity {
                org::OrgMetadataType::Sitemap(_fm) => {
                    let sitemap_item_url = format!(
                        "{}/{}",
                        self.user_config.site.url,
                        util::path_to_string(&v.originating_file_web_path)
                    );
                    let x = LinkData::new(
                        sitemap_item_url,
                        v.originating_file.clone(),
                        LinkMeta::Sitemap,
                        Some(v.front_matter.clone())
                    );
                    out.push(x);
                }
                _ => (),
            }
        }
        out.sort_by_key(|ld| ld.file.clone());
        self.sitemap = out;
    }

    /// render - iterates over all org files and call their render function.
    fn render(&mut self, print_build_log: bool) {
        let _failed_renders: Vec<_> = self
            .org_files
            .par_iter()
            .map(|f| f.render(self).map_err(|e| e))
            .filter_map(|x| x.err())
            .collect();
        if print_build_log {
            println!(
                "{:?} files rendered.",
                self.org_files.len() - _failed_renders.len()
            );

            if !_failed_renders.is_empty() && print_build_log {
                self.print_build_message(_failed_renders);
            }
        }
    }

    /// build_tag_page is responsible for reading the tag page into memory,
    /// inserting the global tags into the tera layout,
    /// and rendering an instance of the page for each tag.
    fn tags_build_pages(&self) {
        // let tera = templates::tera::load_templates(&self.dir_templates);
        fs::create_dir_all(&self.dir_tags).expect("Internal error: failed to create dir_tags.");
        for (tag_name, vec_of_tagged_items) in &self.tags_map {
            let mut ctx = tera::Context::new();
            let mut template_tags: Vec<_> = vec_of_tagged_items
                .iter()
                .map(|i| data::Tag::new(i.to_owned(), self.user_config.site.url.to_string()))
                .collect();

            // filter out tag types if they aren't used
            if !self.user_config.tags.firn {
                template_tags.retain(|f| f.tag_type != "firn")
            }
            if !self.user_config.tags.org {
                template_tags.retain(|f| f.tag_type != "org")
            }

            ctx.insert("tag_name", &tag_name);
            ctx.insert("tagged_items", &template_tags);
            ctx.insert("title", &tag_name);
            ctx.insert("tags", &self.tags_list);
            ctx.insert("sitemap", &self.sitemap);
            ctx.insert("config", &self.user_config);

            let tag_file_name = format!("{}.html", tag_name);
            let out_path = self.dir_tags.join(tag_file_name);
            let output = self.tera.render("[tag].html", &ctx).unwrap();
            fs::write(&out_path, output).expect("failed to write tag file.")
        }
    }

    // --TAGS: End--------------------------------------------------------------

    fn print_build_message(&self, failed_renders: Vec<FirnError>) {
        let mut report: HashMap<FirnErrorType, Vec<FirnError>> = HashMap::new();
        for err in failed_renders {
            let entry = report.entry(err.kind.clone()).or_insert(Vec::new());
            entry.push(err);
        }

        match report.get(&FirnErrorType::IsPrivateFile) {
            Some(priv_files) => {
                if priv_files.len() > 0 {
                    println!("{:?} private files were skipped.", priv_files.len());
                }
            }
            None => (),
        }
    }

    pub fn setup_for_serve(&mut self, port: &str) {
        let port_int = port
            .parse::<u16>()
            .expect("Failed to parse port to an integer");
        self.serve_port = port_int;
        self.user_config.site.url = format!("http://localhost:{}", port);
    }

    fn clean_up_attachments(&self) {
        let mut deleted_files = 0;
        let mut trx_attachments: Vec<String> = Vec::new();
        for f in &self.global_attachments {
            let p = format!(
                "{}/{}",
                util::path_to_string(&self.dir_site_out),
                f.replace("file:", "")
            );
            trx_attachments.push(p);
        }

        let pattern = format!("{}/**/*", util::path_to_string(&self.dir_data_files_dest));
        for entry in glob(&pattern).unwrap() {
            match entry {
                Ok(path) => {
                    let path_meta = fs::metadata(&path).unwrap();
                    if path_meta.is_file()
                        && !trx_attachments.contains(&util::path_to_string(&path))
                    {
                        fs::remove_file(path).unwrap();
                        deleted_files += 1;
                    }
                    // if path is in global links, keep it otherwise delete it.
                }
                Err(_e) => (),
            }
        }
        println!("\nDeleted {:?} unused attachments", deleted_files);

        // loop through all files in _site/folder
    }

    pub fn build(&mut self, print_build_log: bool) -> anyhow::Result<()> {
        if self.dir_firn.exists() {
            self.load_firn_files();
            self.parse_files();
            self.collect_global_data();
            self.cp_data();
            self.cp_static();
            self.tags_build_pages();
            self.render(print_build_log);
            if self.user_config.site.clean_attachments {
                self.clean_up_attachments();
            }
            self.compile_scss()?;
            Ok(())
        } else {
            println!("A '_firn' directory does not exists. Have you run `firn new`?");
            Ok(())
        }
    }

    pub fn rebuild(&mut self, print_build_log: bool) -> anyhow::Result<()> {
        self.global_links.clear();
        self.global_tags.clear();
        self.global_logbook.clear();
        self.global_sitemap.clear();
        self.build(print_build_log)
    }

    pub fn check_site_exists(dir_firn: &PathBuf) {
        if !dir_firn.exists() {
            println!("\nError: A '_firn' directory does not exists. Have you run `firn new`?");
            util::exit();
        }
    }
}
