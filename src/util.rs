use crate::{config::BaseUrl, errors::FirnError};
use glob::glob;
use std::path::{Path, PathBuf};
use tera::Tera;

pub fn load_files(cwd: &Path, pattern: &str) -> Vec<PathBuf> {
    let pattern_path = cwd.join(pattern);
    let pattern_path_str = pattern_path.to_str().unwrap();

    glob(pattern_path_str).unwrap().flatten().collect()
}

/// Turns a space delimited string into a slug, with dashes instead.
pub fn slugify(s: &str) -> String {
    s.replace(" ", "-")
        .replace("=", "-")
        .replace("/", "-")
        .replace(":", "-")
        .replace("\\", "-")
}

pub fn exit() -> ! {
    std::process::exit(1);
}

/// transform_org_link_to_html converts an org link such as:
/// `file:../myorglink.org` to:
/// `<site_baseurl>/myorglink.html`
/// Has to handle:
/// - prepending the baseurl
/// - stripping "../../" from links.
/// - handling links that start with "./" (ie: "sibling lings")
/// which requires the file path of the oroginating link so we can get the parent.
///
pub fn transform_org_link_to_html(
    base_url: BaseUrl,
    org_link_path: String,
    file_path: PathBuf,
) -> String {
    let clean_file_link = |lnk: String| -> String {
        // if it's a link up a directory...
        let mut result = String::from("");
        for i in lnk.split("../") {
            if !i.is_empty() || i != "file:" {
                result = i.to_string();
            }
        }
        str::replace(&result, "file:", "")
    };

    let mut link_path = org_link_path;

    // -- handle different types of links.

    // <1> -- It's a local org file.
    if is_local_org_file(&link_path) {
        link_path = clean_file_link(link_path);
        link_path = str::replace(&link_path, ".org", ".html");
        return base_url.build(link_path, file_path);

    // <2> it's a local image
    } else if is_local_img_file(&link_path) {
        link_path = clean_file_link(link_path);
        return base_url.build(link_path, file_path);
    }

    // <3> is a web link (doesn't start with baseurl.)
    if !is_local_org_file(&link_path) {
        return link_path;
    }
    // <4> We don't know? Just return the link.
    link_path
}


// org link methods
// (mostly for doing things with links that look like:
// `file:../`
// )

pub fn org_str_is_img_link(s: &str) -> bool {
    s.ends_with("jpg")
        || s.ends_with("png")
        || s.ends_with("jpeg")
        || s.ends_with("JPG")
        || s.ends_with("bmp")
        || s.ends_with("gif")
        || s.ends_with("GIF")
        || s.ends_with("PNG")
        || s.ends_with("tiff")
        || s.ends_with("tif")
}

pub fn is_local_file_link(s: &str) -> bool {
    s.starts_with("file:")
}

pub fn is_local_attachment(s: &str) -> bool {
    is_local_file_link(s) && is_local_img_file(s) || s.ends_with(".pdf")
}

pub fn is_local_org_file(link_path: &str) -> bool {
    link_path.ends_with(".org") && is_local_file_link(link_path)
}

pub fn is_local_img_file(s: &str) -> bool {
    org_str_is_img_link(s) && is_local_file_link(s)
}

// get_template
// Returns the name of a template (to later render), provided it's found
// in the tera instance.
pub fn get_template(tera: &Tera, template: &str) -> Result<String, FirnError> {
    let template_with_html = format!("{}.html", &template);
    let default_template_name = "default.html".to_string();
    if tera
        .get_template_names()
        .any(|x| x == template_with_html)
    {
        return Ok(template_with_html);
    }
    Ok(default_template_name)
}

pub fn path_to_string(p: &Path) -> String {
    p.display().to_string()
}
