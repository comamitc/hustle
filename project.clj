(defproject hustle "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]
                 [re-frame "0.7.0"]
                 [secretary "1.2.3"]]

  :min-lein-version "2.5.3"

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.0-6"]
            [lein-doo "0.1.6"]
            [lein-asset-minifier "0.2.4"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]
            [lein-less "1.7.5"]]

   :npm {:dependencies [[source-map-support "0.4.0"]
                        [ws "0.8.1"]
                        [express "4.13.3"]
                        [body-parser "1.15.0"]
                        [serve-static "1.10.2"]
                        [github "0.2.4"]
                        [bitbucket-api "0.1.0"]]}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :repl false}


  :less {:source-paths ["less"]
         :target-path  "resources/public/css/compiled"}

  :minify-assets {:assets {"resources/public/css/compiled/site.min.css"
                           "resources/public/css/compiled/site.css"}
                  :options {:optimizations :advanced}}

  :cljsbuild {:builds [{:id "client"
                        :source-paths ["src/cljs/client" "src/cljs/common"]
                        :figwheel {:on-jsload "client.core/mount-root"}
                        :compiler {:main client.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "client-min"
                        :source-paths ["src/cljs/client" "src/cljs/common"]
                        :compiler {:main client.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}

                       {:id "server"
                        :source-paths ["src/cljs/server" "src/cljs/common"]
                        :figwheel true
                        :compiler {:main server.core
                                   :output-to "target/server/index.js"
                                   :output-dir "target/server"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}

                       ;; @TODO: this is likely to change
                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/test.js"}
                        :main cljs-proj.runner
                        :optimizations :none}]}

  :aliases {"dev" ["pdo" ["less" "auto"]
                         ["figwheel" "client" "server"]]})
