// use crate::front_matter::FrontMatter;
use crate::{html::MyHtmlHandler, util};
use orgize::export::HtmlHandler;
use orgize::{elements, Element};
use serde::Serialize;

// Converts an orgize el of a Link into something renderable.
pub fn orgize_link_to_web_link(
    link: &elements::Link,
    handler: &mut MyHtmlHandler,
    writer: &mut Vec<u8>,
    base_url: String,
) {
    let link_web_path = link_transformer(base_url, link.path.to_owned().to_string());
    let new_link = elements::Link {
        path: std::borrow::Cow::Borrowed(&link_web_path),
        desc: link.desc.to_owned(),
    };
    let new_link_enum = Element::Link(new_link);
     handler.start(writer, &new_link_enum).unwrap()
}

/// Transforms the possible link *paths* we might get while parsing org links from orgize.
pub fn link_transformer(base_url: String, org_link_path: String) -> String {
    let mut link_path = org_link_path;
    // <1> -- It's a local org file.
    if util::is_local_org_file(&link_path) {
        link_path = util::clean_file_link(link_path);
        link_path = str::replace(&link_path, ".org", ".html");
        return util::make_site_url(base_url, link_path)

    // <2> it's a local image
    } else if util::is_local_img_file(&link_path) {
        link_path = util::clean_file_link(link_path);
        return util::make_site_url(base_url, link_path)
    }

    // <3> is a web link (doesn't start with baseurl.)
    if !util::is_local_org_file(&link_path) {
        return link_path
    }
    // <4> We don't know? Just return the link.
    link_path
}


// -- Link data for Tera to loop over --------------------------------------------
//

#[derive(Debug, PartialEq, Serialize)]
pub struct SitemapDate {
    pub date_created: Option<i64>,
    pub date_updated: Option<i64>

}

#[derive(Debug, PartialEq, Serialize)]
pub enum LinkDataKind {
    Backlink,
    RelatedFile,
    Tag(usize),
    Sitemap(SitemapDate)
}

#[derive(Debug, PartialEq, Serialize)]
pub struct LinkData {
    pub path: String,
    pub file: String,
    pub kind: LinkDataKind
}

impl LinkData {
    pub fn new(path: String, file: String, ldk: LinkDataKind) -> LinkData {
        LinkData {
            path,
            file,
            kind: ldk
        }
    }
}
