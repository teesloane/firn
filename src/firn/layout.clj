(ns firn.layout
  "Namespace responsible for using layouts.
  Layouts enable users to have custom layouts for the static site generators.
  This occurs by slurping in some layout files -- which are just `.clj` files, for now
  And then applying them inline.

  NOTE: will change (apply-templates, especially) in the future:
  ; a) probably can't compile down with GRAAL and
  ; b) eval is not a good idea, probably."
  (:require [firn.markup :as markup]
            [firn.org :as org]
            [hiccup.core :as h]))




(defn default-template
  "The default template if no `layout` key is specified.
  This lets users know they need to build a `_layouts/default.clj`"
  [{:keys [curr-file]}]
  [:main
   [:h1 "Note! You don't have a default template."]
   [:div "Please make a _layouts/default.clj file and put it in your org note directory."]
   [:div (markup/to-html (:as-edn curr-file))]])

(defn layout-exists?
  "Checks if a layout for a project exists in the config map
  If it does, return the function value of the layout, otherwise the default template "
  [config layout]
  (get-in config [:layouts layout]
          (get-in config [:layouts :default]
                  default-template)))

(defn with-fns-config
  "Pass functions needed for rendering to configs."
  [config]
  (assoc config
         :render               markup/to-html
         :get-headline         org/get-headline
         :get-headline-content org/get-headline-content))

(defn apply-template
  "If a file has a template, render the file with it, or use the default layout"
  [config layout]
  (let [selected-layout (layout-exists? config layout)]
    (h/html (selected-layout (with-fns-config config)))))
