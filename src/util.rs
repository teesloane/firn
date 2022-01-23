use crate::{errors::FirnError, html::MyHtmlHandler};
use glob::glob;
use orgize::export::HtmlHandler;
use orgize::{elements, Element};
use std::path::{Path, PathBuf};
use tera::Tera;

pub fn load_files(cwd: &Path, pattern: &str) -> Vec<PathBuf> {
    let mut output: Vec<PathBuf> = Vec::new();
    let pattern_path = PathBuf::new().join(&cwd).join(pattern);
    let pattern_path_str = pattern_path.into_os_string().into_string().unwrap();

    for entry in glob(&pattern_path_str).unwrap() {
        match entry {
            Ok(path) => output.push(path),
            Err(_e) => (),
        }
    }
    output
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
    ::std::process::exit(1);
}

// Converts a string: `file:../myorglink.org` to `<site_baseurl>/myorglink.html`
pub fn org_file_link_to_html_link(base_url: String, org_link_path: String) -> String {
    let mut result = clean_file_link(org_link_path);
    result = str::replace(&result, ".org", ".html");
    make_site_url(base_url, result)
}

// removes preceding ../../ from a file: link.
pub fn clean_file_link(org_link_path: String) -> String {
    let mut result = String::from("");
    for i in org_link_path.split("../") {
        if !i.is_empty() || i != "file:" {
            result = i.to_string();
        }
    }
    str::replace(&result, "file:", "")
}

pub fn make_site_url(base_url: String, link: String) -> String {
    format!("{}/{}", base_url, link)
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
    org_str_is_img_link(s.clone()) && is_local_file_link(s.clone())
}

// get_template
// Returns the name of a template (to later render), provided it's found
// in the tera instance.
pub fn get_template(tera: &Tera, template: &str) -> Result<String, FirnError> {
    
    let template_with_html = format!("{}.html", &template);
    let default_template_name = "default.html".to_string();
    if tera.get_template_names().any(|x| x == template_with_html.as_str()) {
        return Ok(template_with_html);
    }
    Ok(default_template_name)
}

pub fn path_to_string(p: &PathBuf) -> String {
    p.display().to_string()
}
