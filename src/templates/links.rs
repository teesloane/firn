use crate::front_matter::FrontMatter;
use serde::Serialize;

/// Transforms the possible link *paths* we might get while parsing org links from orgize.
// pub fn link_transformer(base_url: String, org_link_path: String, file_path: PathBuf) -> String {
//     let mut link_path = org_link_path;
//     // <1> -- It's a local org file.
//     if util::is_local_org_file(&link_path) {
//         link_path = util::clean_file_link(link_path);
//         link_path = str::replace(&link_path, ".org", ".html");
//         return util::make_site_url(base_url, link_path);

//     // <2> it's a local image
//     } else if util::is_local_img_file(&link_path) {
//         link_path = util::clean_file_link(link_path);
//         return util::make_site_url(base_url, link_path);
//     }

//     // <3> is a web link (doesn't start with baseurl.)
//     if !util::is_local_org_file(&link_path) {
//         return link_path;
//     }
//     // <4> We don't know? Just return the link.
//     link_path
// }

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
