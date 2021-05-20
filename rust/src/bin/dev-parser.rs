// This is the script to compile a bin version of the dev parser, rather than
// the jni version.

use orgize::{Org, ParseConfig};
use serde_json::to_string;
use std::env;

fn help() {
    println!(
        "\nFirn development parser usage:\n
dev_parser <string>
    Parse an org-mode string
dev_parser <string-org-keywords> <string-org>
    Parse an org-mode string with custom todo keywords\n"
    );
}

fn main() {
    let args: Vec<String> = env::args().collect();

    match args.len() {
        1 => help(),

        2 => {
            let input = &args[1];
            let org = Org::parse(input);
            println!("{}", to_string(&org).unwrap());
        }

        3 => {
            // let keywords = &args[1];
            let keywords = &args[1];
            let kws: Vec<String> = keywords.split_whitespace().map(|s| s.to_string()).collect();

            let input = &args[2];
            let org = Org::parse_custom(
                input,
                &ParseConfig {
                    // custom todo keywords
                    todo_keywords: (kws, vec![]),
                    ..Default::default()
                },
            );

            println!("{}", to_string(&org).unwrap());
        }

        _ => help(),
    }
}
