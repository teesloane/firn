use crate::front_matter::FrontMatter;
use serde::Serialize;

// -- Link data for Tera to loop over --------------------------------------------

#[derive(Debug, PartialEq, Serialize)]
pub enum LinkMeta {
    Backlink,
    RelatedFile,
    Tag { count: usize },
    Sitemap,
}

#[derive(Debug, PartialEq, Serialize)]
pub struct LinkData {
    pub path: String,
    pub file: String,
    pub meta: LinkMeta,
    pub front_matter: Option<FrontMatter>,
}

impl LinkData {
    pub fn new(
        path: String,
        file: String,
        ldk: LinkMeta,
        front_matter: Option<FrontMatter>,
    ) -> LinkData {
        LinkData {
            path,
            file,
            meta: ldk,
            front_matter,
        }
    }
}
