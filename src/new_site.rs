use std::collections::HashMap;
use std::fs;
use std::path::PathBuf;

const CONFIG_YAML: &str = r#"# site-wide configuration:
site:
  # your site url; this will become a localhost url when using `firn serve`
  url: "http://localhost:8080"
  # site title
  title: "My Site"
  # your site description.
  description: "My Site Description"
  # directories to ignore org files in when rendering.
  ignored_directories: []
  # relative root to a folder where org attachments are stored
  data_directory: "data"
  # deletes all unlinked attachments from _site folder after build
  clean_attachments: false
  # can be "scss" or "sass"
  sass: "scss"

# Per-file customization:
# currently not possible, but eventually users will be able to
# control rendering variables on a per-file basis.
file:
  table_of_contents: "no"
  todo_keywords: ["TODO", "DONE"]

# Tags ---

tags:
  # enable this if you want Firn to create an html page for every tag you have;
  # the contents of which can be customized in the [tags].html file Firn generates
  create_tag_pages: true

  # customize this to be the path that tags are filed under. Do this if you want to use
  # a word other than "tags" (such as "subjects" or "categories").
  # note: must end in a forward slash.
  url: "tags/"

  # Set `org` to true if you want to create a [tag].html page for every *org-mode* tag.
  org: false

  # Set `firn` to true if you want to create a [tag].html page for every *firn_tag* front matter.
  firn: true
"#;

const TAG_TEMPLATE: &str = r#"{% import "macros.html" as macros %}
<html>
  {% include "partials/head.html" %}
  <body style="display: flex;">
    <main style="width: 600px; margin: 0 auto; padding: 32px;">
    <div>
      <h1>{{tag_name | capitalize}}</h1>
      {# Example: Render a list of pages that use the current firn/roam tag. #}
      {% for tag in tagged_items %}
        {% if tag.tag_type == "firn" %}
          <li>
            <a href="{{tag.path}}">{{tag.title}}</a>
          </li>
        {% endif %}
      {% endfor %}


      {# The below code is commented out.

        If you wish to also render tags from org headline tags,
        use something like the code below
        and set tags > org = true in config.yaml.

      <h3>Headline Tags</h3>
      {% for tag in tagged_items %}
        {% if tag.tag_type == "org" %}
          <li>
            <a href="{{tag.path}}">{{tag.title}}</a>
          </li>
        {% endif %}
      {% endfor %}
      #}

    </div>
    </main>

    <aside style="padding: 32px; width: 300px;">
      {{macros::link_list(title="Tags", list_items=tags)}}
    </aside>
  </body>
</html>
"#;

const PARTIAL_HEAD: &str = r#"<html>
  <head>
    <meta charset="utf-8">
    <title>{{ title }} - My Site</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="apple-touch-icon" href="/apple-touch-icon.png">
    <script src="{{config.site.url}}/static/js/main.js"></script>
    <link rel="stylesheet" href="{{config.site.url}}/static/css/main.css" type="text/css" media="screen" />
    <style>
    </style>
  </head>
"#;

const PARTIAL_RECENT: &str = r#"{# NOTE! This partial will fail to render if files are missing
their #+date_created and #+date_updated frontmatter. #}
{% if sitemap | length > 0 %}
<section>
  <div>Recently Published</div>
  <ul>
    {% for i in sitemap | sort(attribute="kind.Sitemap.date_created") | reverse | slice(end=10) %}
      <li><a href={{i.path}}>{{i.file}}</a></li>
    {% endfor %}
  </ul>
</section>

<section>
  <div>Recently Updated</div>
  <ul>
    {% for i in sitemap | sort(attribute="kind.Sitemap.date_updated") | reverse | slice(end=10) %}
      <li><a href={{i.path}}>{{i.file}}</a></li>
    {% endfor %}
  </ul>
</section>
{% endif %}
"#;

const MACROS: &str = r#" {# the link_list macro is used to render a generic list of links (backlinks, sitemap, tags etc.) #}
{% macro link_list(title, list_items) %}
  {% if list_items | length > 0 %}
    <section>
      <details open>
        <summary>{{title}}</summary>
        <ul>
          {% for item in list_items %}
            <li>
              <a href={{item.path}}>{{item.file | capitalize }}
                {% if item.kind.Tag %} ({{item.kind.Tag}}) {% endif %}
              </a>
            </li>
          {% endfor %}
        </ul>
      </details>
    </section>
  {% endif %}
{% endmacro input %}

"#;

const DEFAULT_HTML: &str = r#"{% import "macros.html" as macros %}
<html>
  {% include "partials/head.html" %}
  <body style="display: flex;">
    <main style="width: 600px; margin: 0 auto; padding: 32px;">
      {{render()}}
    </main>

    <aside style="padding: 32px; width: 300px;">
     <section>{{toc()}}</section>
      {{macros::link_list(title="Backlinks", list_items=backlinks)}}
      {{macros::link_list(title="Related", list_items=related)}}
      {{macros::link_list(title="Sitemap", list_items=sitemap)}}
      {{macros::link_list(title="Tags", list_items=tags)}}
    </aside>
  </body>
</html>
"#;

const DEFAULT_JS: &str = r#"
"#;

const DEFAULT_SCSS: &str = r#"body{
  color: #333;
  background: #efefef;
}

section {
  padding-bottom: 32px;
}

ul, ol {
  padding-left: 16px;
}

img { max-width: 600px; }
"#;

pub fn init(cwd: PathBuf) {
    let dir_firn = PathBuf::new().join(&cwd).join("_firn");
    if fs::metadata(dir_firn.clone()).is_ok() {
        println!("A '_firn' site already exists at this directory.")
    } else {
        let dirs = vec!["layouts/", "layouts/partials", "sass", "static/css", "static/js", "_site"];

        let mut files = HashMap::new();
        files.insert(String::from("layouts/partials/head.html"), PARTIAL_HEAD);
        files.insert(String::from("layouts/macros.html"), MACROS);
        files.insert(String::from("layouts/partials/recent.html"), PARTIAL_RECENT);
        files.insert(String::from("static/js/main.js"), DEFAULT_JS);
        files.insert(String::from("sass/main.scss"), DEFAULT_SCSS);
        files.insert(String::from("layouts/default.html"), DEFAULT_HTML);
        files.insert(String::from("layouts/[tag].html"), TAG_TEMPLATE);
        files.insert(String::from("config.yaml"), CONFIG_YAML);

        // Map over the above strings, turn them into paths, and create them.
        for dir in dirs.iter() {
            let joined_dir = dir_firn.join(dir);
            fs::create_dir_all(joined_dir).expect("Couldn't create a new firn, directory");
        }

        for (filename, file_contents) in files {
            let joined_dir = dir_firn.join(filename);
            fs::write(joined_dir, file_contents).expect("Unable to write new site layout files.");
        }
    }
}
