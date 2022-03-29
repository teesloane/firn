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



pub fn transform_org_link_to_html(
    base_url: BaseUrl,
    org_link_path: String,
    file_path: PathBuf,
) -> String {
    let num_parents = 0;

    let mut link_path = org_link_path;

    // -- handle different types of links.

    // <1> -- It's a local org file.
    if is_local_org_file(&link_path) {
        // link_path = clean_file_link(link_path);
        link_path = str::replace(&link_path, ".org", ".html");
        link_path = str::replace(&link_path, "file:", "");
        let x = base_url.build(link_path, file_path, num_parents);
        return x

    // <2> it's a local image
    } else if is_local_img_file(&link_path) {
        link_path = str::replace(&link_path, "file:", "");
        return base_url.build(link_path, file_path, num_parents);
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
    if tera.get_template_names().any(|x| x == template_with_html) {
        return Ok(template_with_html);
    }
    Ok(default_template_name)
}

pub fn path_to_string(p: &Path) -> String {
    p.display().to_string()
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn test_is_local_file_link() {
        assert_eq!(true, is_local_file_link("file:assimil.org"));
        assert_eq!(false, is_local_file_link("https://theiceshelf.com"));
    }

    #[test]
    fn test_transform_org_link_to_html() {
        let base_url = BaseUrl::new(
            "https://mysite.com".to_string(),
            PathBuf::from("/Users/pi/firnsite"),
            PathBuf::from("/Users/pi/firnsite/wiki/_firn/_site/data"),
        );

        assert_eq!(
            "https://mysite.com/sibling_file.html",
            transform_org_link_to_html(
                base_url.clone(),
                "file:sibling_file.org".to_string(),
                "/Users/pi/firnsite/myfile.org".into()
            )
        );
        assert_eq!(
            "https://mysite.com/blog/child_file.html",
            transform_org_link_to_html(
                base_url.clone(),
                "file:blog/child_file.org".to_string(),
                "/Users/pi/firnsite/myfile.org".into()
            )
        );

        assert_eq!(
            "https://mysite.com/data/files/blog/2022/visitor_3.gif",
            transform_org_link_to_html(
                base_url.clone(),
                "file:../data/files/blog/2022/visitor_3.gif".to_string(),
                "/Users/pi/firnsite/blog/visitor3.org".into()
            )
        );

        assert_eq!(
            "https://mysite.com/parent_file.html",
            transform_org_link_to_html(
                base_url.clone(),
                "file:../../parent_file.org".to_string(),
                "/Users/pi/firnsite/nested/deeper/myfile.org".into()
            )
        );

        assert_eq!(
            "https://mysite.com/nested/parent_file.html",
            transform_org_link_to_html(
                base_url.clone(),
                "file:../parent_file.org".to_string(),
                "/Users/pi/firnsite/nested/deeper/myfile.org".into()
            )
        );

        assert_eq!(
            "https://mysite.com/blog/nested_sibling_file.html",
            transform_org_link_to_html(
                base_url.clone(),
                "file:nested_sibling_file.org".to_string(),
                "/Users/pi/firnsite/blog/myfile.org".into()
            )
        )
    }
}
