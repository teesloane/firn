/* Sitemap.rs: Tree-like structure for rendering sitemap */
use crate::templates::links::LinkData;
use serde::Serialize;
use std::ffi::OsString;
use std::path::PathBuf;

#[derive(Debug, PartialEq)]
pub struct SitemapTree {
    pub dummyroot: usize, // 0 is the dummyroot
    pub nodes: Vec<SitemapTreeNode>,
}

#[derive(Debug, PartialEq, Serialize)]
pub struct SitemapTreeNode {
    pub path_comp: String, // the path component that this node represents
    pub data: Option<LinkData>,
    pub next_sibling: Option<usize>,
    pub first_child: Option<usize>,
    pub last_child: Option<usize>,
}

impl SitemapTreeNode {
    pub fn add_child(&mut self, child: usize) {
        if self.first_child == None {
            self.first_child = Some(child);
        }
        self.last_child = Some(child);
    }

    pub fn add_next_sibling(&mut self, sibling: usize) {
        self.next_sibling = Some(sibling);
    }
}

impl SitemapTree {
    pub fn new() -> SitemapTree {
        let nodes = vec![SitemapTreeNode {
            path_comp: String::from(""),
            data: None,
            next_sibling: None,
            first_child: None,
            last_child: None,
        }];
        SitemapTree {
            dummyroot: 0,
            nodes,
        }
    }
    pub fn append(
        &mut self,
        os_path_comp: OsString,
        data: Option<LinkData>,
        parent: usize,
    ) -> usize {
        // Get the next free index
        let next_index = self.nodes.len();
        let child = next_index;
        let path_comp = os_path_comp.into_string();
        self.nodes.push(SitemapTreeNode {
            path_comp: path_comp.unwrap(),
            data,
            next_sibling: None,
            first_child: None,
            last_child: None,
        });
        self.nodes[parent].add_child(next_index);
        child
    }
    pub fn insert(&mut self, p: PathBuf, data: LinkData) {
        let comps: Vec<_> = p.components().map(|comp| comp.as_os_str()).collect();
        let mut i = 0;
        let mut current_node = Some(self.dummyroot);
        'traverse: loop {
            if current_node == None || i == comps.len() {
                break 'traverse;
            }
            let mut last_child = None;
            let mut child = self.nodes[current_node.unwrap()].first_child;
            'traverse_children: loop {
                match child {
                    None => {
                        let new_node;
                        if i < comps.len() - 1 {
                            new_node =
                                self.append(comps[i].to_os_string(), None, current_node.unwrap());
                        } else {
                            // it's a leaf
                            new_node = self.append(
                                comps[i].to_os_string(),
                                Some(data.clone()),
                                current_node.unwrap(),
                            );
                        };

                        if let Some(x) = last_child {
                            // it's not the first child
                            let v: &mut SitemapTreeNode = &mut self.nodes[x];
                            v.add_next_sibling(new_node);
                        }
                        child = Some(new_node);
                        break 'traverse_children;
                    }
                    Some(x) => {
                        let comp = self.nodes[x].path_comp.clone();
                        if OsString::from(comp) == comps[i] {
                            break 'traverse_children;
                        } else {
                            last_child = child;
                            child = self.nodes[child.unwrap()].next_sibling;
                        }
                    }
                }
            }
            current_node = child;
            i += 1;
        }
    }
}
