(defproject htmx_lnl "0.1.0-SNAPSHOT"
  :description "Small card tracking web app"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.11.0"]
                 [ring/ring-devel "1.11.0"]
                 [ring/ring-jetty-adapter "1.11.0"]
                 [metosin/reitit-core "0.7.0-alpha7"]
                 [metosin/reitit-ring "0.7.0-alpha7"]
                 [hiccup/hiccup "2.0.0-RC2"]
                 [faker/faker "0.2.2"]]
  :repl-options {:init-ns htmx-lnl.core})
