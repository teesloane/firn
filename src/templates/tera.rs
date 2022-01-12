use crate::{config::Config, org::OrgFile, templates, util};
use std::path::PathBuf;
use tera::Tera;

/// setup_tera
/// Registers everything we need and returns the tera instance.
///
///
/// TODO: refactor unnecesary cloning of tera? I clone the tera on the config
/// because... well... before I was setting it up and reading the templates
/// every time I rendered an org file but that seemed inefficient. So, instead,
/// I tried instantiating tera once, and setting it on the config. This worked,
/// but in order to overwrite the registered_functions on tera, the config
/// needed to be mutable. And Rust was unhapy with that. But, I thought cloning
/// was better than just reading in the template files over and over again per
/// file. So... refactor later when I better understand Rust things. NOTE: It
/// might be possible that I could just connect the tera template trait impl to
/// the original structs for OrgFile?
pub fn setup(cfg: &Config, org_file: &OrgFile) -> Tera {
    let mut tera = cfg.tera.clone();
    // Register our functions.
    tera.register_function("render", templates::Render::new(org_file, cfg));
    tera.register_function(
        "toc",
        templates::Toc::new(org_file, cfg.user_config.clone()),
    );
    // More templates to come later...
    // tera.register_function("logbook", templates::Render::new(&org_file));
    tera
}

pub fn load_templates(dir_templates: &PathBuf) -> Tera {
    let template_path = format!("{}/**/*.html", util::path_to_string(dir_templates));
    let mut tera = match Tera::new(&template_path) {
        Ok(t) => t,
        Err(e) => {
            println!("Parsing error(s): {}", e);
            util::exit();
        }
    };
    tera.autoescape_on(vec![]);
    if tera.templates.is_empty() {
        println!("\nError: No templates found in {:?}\n", template_path);
        util::exit();
    }
    tera
}
