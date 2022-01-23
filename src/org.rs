use crate::config::Config;
use crate::errors::{FirnError, FirnErrorType};
use crate::front_matter::{self, FrontMatter};
use crate::{templates, util};

use chrono::{Duration, NaiveTime};
use orgize::elements::Clock;
use orgize::{elements, Element, Event, Org};
use serde::Serialize;
use slugify::slugify;
use std::fs;
use std::path::PathBuf;
use tera::Context;

#[derive(Debug, PartialEq, Eq, Serialize, Clone)]
pub enum OrgTagType {
    FirnTag, // firn front matter tag
    OrgTag,  // Org headline tag.
}

/// OrgEntity/OrgMetadata
/// The OrgFile struct collects metadata, like tags, clocks and links etc.
/// However, when we do that we need to associate WHERE that data was coming from.
/// If it's a tag, we want to get the originating title, etc.
/// This information gets used later when we access global tags, logs, etc.
#[derive(Debug, Serialize, Clone)]
pub enum OrgMetadataType<'a> {
    Clock(elements::Clock<'a>),
    Link(elements::Link<'a>),
    Tag(String, OrgTagType),
    Sitemap(FrontMatter),
}

#[derive(Debug, Serialize, Clone)]
pub struct OrgMetadata<'a> {
    pub entity: OrgMetadataType<'a>,
    pub originating_file: String,
    pub originating_file_path: PathBuf,
    pub originating_file_web_path: PathBuf,
    pub originating_headline: Option<String>,
    pub originating_headline_web_path: Option<String>,
    pub front_matter: FrontMatter,
}

impl<'a> OrgMetadata<'a> {
    pub fn new(
        entity: OrgMetadataType<'a>,
        title: Option<&elements::Title>,
        web_path: &PathBuf,
        file_path: &PathBuf,
        front_matter: &front_matter::FrontMatter,
    ) -> OrgMetadata<'a> {
        let file_title = front_matter.title.clone();
        let originating_file_web_path = web_path.clone();
        let originating_file_path = file_path.to_path_buf();
        let originating_file = file_title.unwrap_or(util::path_to_string(&originating_file_path));
        // TODO: write a function that cleans headlines - removes links, etc etc
        // etc - from raw. Headline stuff - not every metadata has an associated
        // headline necessarily (ie, links in an org doc before a headline
        // occurs).
        if let Some(title) = title {
            let slugged_headline = slugify!(&title.raw.clone());
            let originating_headline = Some(title.raw.clone().to_string());
            let originating_headline_web_path = Some(format!(
                "{:}#{:}",
                &originating_file_web_path.as_os_str().to_str().unwrap(),
                slugged_headline
            ));

            return OrgMetadata {
                entity,
                originating_file,
                originating_file_path,
                originating_file_web_path,
                originating_headline,
                originating_headline_web_path,
                front_matter: front_matter.clone(),
            };
        }
        OrgMetadata {
            entity,
            originating_file,
            originating_file_path,
            originating_file_web_path,
            originating_headline: None,
            originating_headline_web_path: None,
            front_matter: front_matter.clone(),
        }
    }

    pub fn get_web_path_as_str(&self) -> String {
        util::path_to_string(&self.originating_file_web_path)
    }
}

// OrgFile
//

// Here we collect everything we need for each file:
// The parsing of org content, of course,
// links, logbooks, tags, frontmatter, etc.
//
// FIXME: Big refactor opportunity: caching parsed orgize content.
// I am often parsing org content several times inside of templates
// - because I can't figure out how to get this parsed structure, which
// requires lifetimes, to be able be accessed in Tera templates. So, each
// tera template ends up having to re-parse the file when that respective
// function is used - so if `render` and `toc` functions are called it ends
// up parsing the org string two more times than necessary.
#[derive(Serialize)]
pub struct OrgFile<'a> {
    pub file_path: PathBuf,
    pub out_path: PathBuf,
    pub web_path: PathBuf,
    pub full_url: String,
    pub parsed: orgize::Org<'a>,
    pub original_org: String,
    pub front_matter: FrontMatter,
    pub links: Vec<OrgMetadata<'a>>,
    pub logbook: Vec<OrgMetadata<'a>>,
    pub sitemap_data: OrgMetadata<'a>,
    pub tags: Vec<OrgMetadata<'a>>,
    pub attachments: Vec<String>,
    pub posts: Vec<OrgFile<'a>>,
}

impl<'a> OrgFile<'a> {
    /// new takes a read file and it's path and returns an OrgFile struct
    /// which is full of parsed data from iterating over the parsed output of orgize.
    // pub fn new(read_file: String, cwd: PathBuf, file_path: PathBuf) -> OrgFile<'a> {
    pub fn new(read_file: String, cfg: &Config, file_path: PathBuf) -> OrgFile<'a> {
        let original_org = read_file.clone();
        let web_path = file_path
            .strip_prefix(&cfg.dir_source)
            .unwrap()
            .with_extension("html");
        let web_path_str = web_path
            .clone()
            .into_os_string()
            .into_string()
            .expect("Failed to convert web_path to string");
        let full_url = cfg.base_url.clone().build(web_path_str, file_path.clone());
        let out_path = PathBuf::from(&cfg.dir_site_out).join(&web_path);
        let parsed = Org::parse_string(read_file);
        let front_matter = FrontMatter::new(&parsed);
        let sitemap_data = OrgMetadata::new(
            OrgMetadataType::Sitemap(front_matter.clone()),
            None,
            &web_path,
            &file_path,
            &front_matter,
        );

        let (links, logbook, tags, attachments) =
            OrgFile::collect_data(&parsed, &web_path, &file_path, front_matter.clone());

        OrgFile {
            attachments,
            file_path,
            out_path,
            web_path,
            full_url,
            parsed,
            original_org,
            front_matter,
            links,
            logbook,
            sitemap_data,
            posts: Vec::new(),
            tags,
        }
    }

    /// collect_data loops through the content of a parsed org file and collects all logbooks
    /// and links, to be pulled into the full config
    pub fn collect_data(
        parsed_org: &Org<'a>,
        web_path: &PathBuf,
        file_path: &PathBuf,
        front_matter: front_matter::FrontMatter,
    ) -> (
        Vec<OrgMetadata<'a>>,
        Vec<OrgMetadata<'a>>,
        Vec<OrgMetadata<'a>>,
        Vec<String>,
    ) {
        let mut links: Vec<OrgMetadata> = Vec::new();
        let mut clocks: Vec<OrgMetadata> = Vec::new();
        let mut tags: Vec<OrgMetadata> = Vec::new();
        let mut attachments: Vec<String> = Vec::new();
        let mut most_recent_title: Vec<elements::Title> = Vec::new();
        let make_metadata = |metadata_type, title: Option<&elements::Title>| {
            OrgMetadata::new(metadata_type, title, web_path, file_path, &front_matter)
        };
        if front_matter.is_public() {
            // loop over front matter firn tags and push them into global tags.
            front_matter.firn_link_to_org_metadata(web_path, file_path, &mut tags);

            // loop over content and collect metdata.
            for event in parsed_org.iter() {
                match event {
                    Event::Start(element) => match element {
                        Element::Title(hl) => {
                            most_recent_title = vec![hl.clone()];
                            for tag in &hl.tags {
                                tags.push(make_metadata(
                                    OrgMetadataType::Tag(tag.to_string(), OrgTagType::OrgTag),
                                    most_recent_title.get(0),
                                ))
                            }
                        }
                        Element::Clock(ts) => {
                            if ts.is_closed() {
                                clocks.push(make_metadata(
                                    OrgMetadataType::Clock(ts.clone()),
                                    most_recent_title.get(0),
                                ))
                            }
                        }
                        Element::Link(l) => {
                            // if attachments, push to attachment vec.
                            if util::is_local_attachment(&l.path.clone()) {
                                attachments.push(l.path.clone().to_string());
                            }

                            links.push(make_metadata(
                                OrgMetadataType::Link(l.clone()),
                                most_recent_title.get(0),
                            ));
                        }
                        _ => {
                            ();
                        }
                    },
                    Event::End(_element) => {}
                }
            }
        }
        (links, clocks, tags, attachments)
    }

    pub fn is_in_private_folder(&self, ignored_dirs: &Vec<String>, dir_source: &PathBuf) -> bool {
        let dir_source = util::path_to_string(dir_source);
        let ignored_dirs: Vec<String> = ignored_dirs
            .iter()
            .map(|d| format!("{}/{}", dir_source, d))
            .collect();
        let mut ancestors = self.file_path.ancestors();
        ancestors.any(|f| ignored_dirs.contains(&f.display().to_string()))
    }

    pub fn is_private(&self, ignored_dirs: &Vec<String>, dir_source: &PathBuf) -> bool {
        self.is_in_private_folder(ignored_dirs, dir_source) || self.front_matter.firn_private
    }

    /// [stub] validate_file runs various checks to make sure that the file can be rendered.
    /// - does it have the requisite front matter (title...)
    pub fn valid_for_rendering(&self) -> Result<bool, FirnError> {
        if let None = &self.front_matter.title {
            return Err(FirnError::new(
                &format!(
                    "{} {}",
                    "No title found for: ".to_string(),
                    self.file_path.display().to_string()
                ),
                FirnErrorType::FrontMatterNoTitle,
            ));
        }

        if self.front_matter.is_private() {
            return Err(FirnError::new(
                &format!(
                    "{} {}",
                    "Private file: ".to_string(),
                    self.file_path.display().to_string()
                ),
                FirnErrorType::IsPrivateFile,
            ));
        }

        Ok(true)
    }

    fn get_logbook_sum(&self) -> chrono::Duration {
        let mut accumulator = Duration::zero();
        for log in &self.logbook {
            match &log.entity {
                OrgMetadataType::Clock(clock) => {
                    match clock {
                        Clock::Closed {
                            start,
                            end,
                            repeater: _,
                            delay: _,
                            duration: _,
                            post_blank: _,
                        } => {
                            let start: NaiveTime = start.into();
                            let end: NaiveTime = end.into();
                            let diff = end - start;
                            accumulator = accumulator + diff;
                        }
                        // we don't handle clocks running
                        Clock::Running {
                            start: _,
                            repeater: _,
                            delay: _,
                            post_blank: _,
                        } => (),
                    }
                }
                _ => (),
            }
        }
        accumulator
    }

    /// get_related_files
    /// Loops over all global tags
    /// then loops over the firn_tags/roam_tags for the current file on self.
    /// if the global tag matches one of the firn tags and it's not the current file
    /// then we keep that as a "related link".
    fn get_related_files(&self, cfg: &Config) -> Vec<templates::links::LinkData> {
        let mut out: Vec<_> = Vec::new();

        for g_tag in &cfg.global_tags {
            // if any global tags match any of the tags of this file, include 'em.
            match &g_tag.entity {
                OrgMetadataType::Tag(global_tag, _) => {
                    for local_tag in &self.tags {
                        match &local_tag.entity {
                            OrgMetadataType::Tag(local_tag_name, local_tag_type) => {
                                let related_item_url = format!(
                                    "{}/{}",
                                    cfg.user_config.site.url,
                                    util::path_to_string(&g_tag.originating_file_web_path)
                                );

                                if let OrgTagType::FirnTag = local_tag_type {
                                    if local_tag_name == global_tag
                                        && local_tag.originating_file_path
                                            != g_tag.originating_file_path
                                    {
                                        let new_link = templates::links::LinkData::new(
                                            related_item_url,
                                            g_tag.originating_file.clone(),
                                            templates::links::LinkMeta::RelatedFile,
                                            Some(self.front_matter.clone()),
                                        );
                                        if !out.contains(&new_link) {
                                            out.push(new_link);
                                        }
                                    }
                                }
                            }
                            _ => (),
                        }
                    }
                }
                _ => (),
            }
        }
        out
    }

    /// get_backlinks iterates over every global link
    /// and returns a list of links that links to the current file.
    fn get_backlinks(&self, cfg: &Config) -> Vec<templates::links::LinkData> {
        let mut out: Vec<_> = Vec::new();
        for g_link in &cfg.global_links {
            // if the weblink matches self's web_path it's a match.
            match &g_link.entity {
                OrgMetadataType::Link(link) => {
                    let new_link_path = link.path.to_owned().to_string();
                    let web_link =
                        // util::org_file_link_to_html_link(cfg.clone_baseurl(), new_link_path, self.file_path);
                        // TODO: find out if this stuff works still
                        util::transform_org_link_to_html(cfg.base_url.clone(), new_link_path, self.file_path.clone());

                    let backlink_item_url = format!(
                        "{}/{}",
                        cfg.user_config.site.url,
                        util::path_to_string(&g_link.originating_file_web_path)
                    );

                    if web_link == self.full_url {
                        let new_link = templates::links::LinkData::new(
                            backlink_item_url,
                            g_link.originating_file.clone(),
                            templates::links::LinkMeta::Backlink,
                            Some(self.front_matter.clone()),
                        );
                        if !out.contains(&new_link) {
                            out.push(new_link);
                        }
                    }
                }
                _ => (),
            }
        }
        out
    }

    /// Sets up our templates with all the values they might need
    fn setup_tera_ctx(&self, ctx: &mut tera::Context, cfg: &Config) {
        let logbook_sum = self.get_logbook_sum();
        ctx.insert("backlinks", &self.get_backlinks(cfg));
        ctx.insert("title", &self.front_matter.get_title());
        ctx.insert("frontmatter", &self.front_matter);
        ctx.insert("related", &self.get_related_files(cfg));
        ctx.insert("logbook", &logbook_sum.num_hours());
        ctx.insert("sitemap", &cfg.sitemap);
        ctx.insert("config", &cfg.user_config);
        ctx.insert("tags", &cfg.tags_list);
    }

    /// get_breadcrumbs -> a list of links to parent, grandparent etc.
    /// Loops through fm's "firn_under", and finds the file link
    fn _get_breadcrumbs(&self, _cfg: &Config) {}

    /// TODO _render_posts will be responsible for iterating over headlines
    /// ⚠️ This function is a construction site.
    /// and accessing their properties / contents etc, and then using that
    /// to construct new "OrgFile" structs, which will be consiered "Posts"
    /// Allowing for the possibility of rendering multiple html files
    /// from a single org file (use case: a blog.)
    fn _render_posts(&self) {
        for hl in self.parsed.headlines() {
            match hl.section_node() {
                Some(node_id) => {
                    let x = self.parsed.arena()[node_id].get();
                    println!(">>> {:?}", x);
                    // gself.parsed.index_mut(nodeId);
                }
                None => (),
            }
            // let parsed = hl.children(&self.parsed);
            // let front_matter = FrontMatter::new_from_properties_map(hl.title(parsed).properties);

            // let original_org = read_file.clone();
            // let web_path = file_path
            //     .strip_prefix(&cfg.dir_source)
            //     .unwrap()
            //     .with_extension("html");
            // let web_path_str = web_path
            //     .clone()
            //     .into_os_string()
            //     .into_string()
            //     .expect("Failed to convert web_path to string");
            // let full_url = util::make_site_url(cfg.clone_baseurl(), web_path_str);
            // let out_path = PathBuf::from(&cfg.dir_site_out).join(&web_path);
            // let parsed = Org::parse_string(read_file);
            // let front_matter = FrontMatter::new(&parsed);
            // let sitemap_data = OrgMetadata::new(
            //     OrgMetadataType::Sitemap(front_matter.clone()),
            //     None,
            //     &web_path,
            //     &file_path,
            //     &front_matter,
            // );

            // let (links, logbook, tags, attachments) =
            //     OrgFile::collect_data(&parsed, &web_path, &file_path, front_matter.clone());

            // let post_as_orgfile = OrgFile {
            //     file_type: front_matter.get_file_type(),
            //     attachments,
            //     file_path,
            //     out_path,
            //     web_path,
            //     full_url,
            //     parsed,
            //     original_org,
            //     front_matter,
            //     links,
            //     logbook,
            //     sitemap_data,
            //     posts: Vec::new(),
            //     tags,
            // };

            // &self.posts.push(post_as_orgfile);
        }
    }

    /// render spits out html to disk.
    pub fn render(&self, cfg: &Config) -> Result<(), FirnError> {
        self.valid_for_rendering()?;
        let tera = templates::tera::setup(cfg, self);
        let template_name = util::get_template(&cfg.tera, &self.front_matter.get_layout())?;
        let mut ctx = Context::new();
        self.setup_tera_ctx(&mut ctx, cfg);

        if !self.is_private(&cfg.user_config.site.ignored_directories, &cfg.dir_source) {
            let tera_output = tera
                .render(&template_name, &ctx)
                // .render("default.html", &ctx)
                .expect("Failed to render template.");
            let parent = self.out_path.parent().expect("File had no parent");
            fs::create_dir_all(parent).expect("Failed to create file out_path.");
            fs::write(&self.out_path, tera_output).expect("Failed to write file.");
        }
        Ok(())
    }
}
