pub mod config;
pub mod errors;
pub mod front_matter;
pub mod html;
pub mod new_site;
pub mod org;
pub mod serve;
pub mod templates;
pub mod user_config;
pub mod util;

use std::path::PathBuf;

use anyhow::Result;
use clap::{Parser, Subcommand};

/// Org Mode static site generator
#[derive(Parser)]
#[clap(name = "Firn", version = "0.15", author = "The Ice Shelf")]
struct Cli {
    /// Set the level of verbosity, e.g. -v => verbose, -vv => very verbose
    #[clap(short, long, global = true, parse(from_occurrences))]
    verbose: u8,

    #[clap(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Scaffold files and folders needed to start a new site
    New {
        /// The target directory for a new site
        #[clap(default_value = ".")]
        path: PathBuf,
    },

    /// Build a static site
    Build {
        /// Directory containing files to be built
        #[clap(default_value = ".")]
        path: PathBuf,
    },

    /// Run a development server for processed org files
    Serve {
        /// The port for the development server to run on
        #[clap(short, long, default_value_t = 8080)]
        port: u16,

        /// Directory containing files to be built and served
        #[clap(default_value = ".")]
        path: PathBuf,
    },
}

fn main() -> Result<()> {
    let cli = Cli::parse();

    match cli.command {
        Command::New { path } => new_site::init(path),
        Command::Build { path } => {
            let mut config = unwrap_config(path, cli.verbose);
            config.build(true)?;
        }
        Command::Serve { port, path } => {
            let mut config = unwrap_config(path, cli.verbose);
            config.setup_for_serve(port);
            config.build(true)?;
            serve::start_server(&mut config);
        }
    }
    Ok(())
}

fn unwrap_config(cwd_as_path: PathBuf, verbosity: u8) -> config::Config<'static> {
    let config = match config::Config::new(cwd_as_path, verbosity) {
        Ok(cfg) => cfg,
        Err(_e) => {
            println!("{:?}", _e);
            util::exit();
        }
    };
    config
}
