(ns firn.core-test
  (:require [firn.build :as build]
            [me.raynes.fs :as fs]
            [firn.util :as u]
            [firn.config :as config]))

(when u/dev?
  (def test-dir      "test/firn/demo_org")
  (def wiki-dir      "/Users/tees/Dropbox/wiki")

  (defn build-test-files
    [dir-to-build]
    (fs/delete-dir (config/make-dir-firn dir-to-build))
    (build/new-site {:dir-files dir-to-build})
    (build/all-files {:dir-files dir-to-build}))

  (build-test-files test-dir)
  (build-test-files wiki-dir))
