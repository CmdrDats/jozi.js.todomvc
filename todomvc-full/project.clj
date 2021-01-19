(defproject jozi.js.todomvc "0.1.0-SNAPSHOT"
  :description "Quick demo implementation of TodoMVC with Clojure/Script"
  :url "http://github.com/cmdrdats/jozi.js.todomvc"
  :license 
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [org.clojure/clojurescript "1.10.773"]

   [http-kit "2.5.0"]
   [ring "1.8.2"]
   [ring/ring-anti-forgery "1.3.0"]
   [compojure "1.6.2"]

   [rum "0.12.3"]
   [com.taoensso/sente "1.16.0"]
   [inflections "0.13.2"]]
  :main todomvc.core

  :repl-options
  {:init-ns todomvc.core
   :init (todomvc.core/-main)})
