# todomvc

Jozi.js todomvc: https://app.pitch.com/app/presentation/43f3603f-1282-4d32-9c9e-98ece5840d4f/f109480e-3da4-40c5-a5e3-5ed0cba6b376



## Usage

# Install 

- If you haven't already, get node, npm & java jdk installed.. 

- shadow-cljs (https://shadow-cljs.github.io/docs/UsersGuide.html#_installation)
```
sudo npm install -g shadow-cljs
```

- lein (https://leiningen.org/)
```
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod a+x lein
mv lein /usr/local/bin
lein
```

# Create project

Just copy the `todomvc-blank` to `todomvc`. If you want to see the final thing, head to
`todomv-full` and follow the instructions for running it below

See the package.json, project.clj and shadow-cljs.edn files for the 


# Run 

```
npm install
shadow-cljs watch dev
```

Then, in another tab,

```
lein repl
```

Browse to http://localhost:8040 - and you should be ready to roll.

# Dependency links

- https://http-kit.github.io/ - HTTP Server
- https://github.com/ring-clojure/ring - HTTP Middleware
- https://github.com/weavejester/compojure - HTTP Routing
- https://github.com/tonsky/rum - React + rendering
- https://github.com/ptaoussanis/sente - WebSockets
- https://github.com/ring-clojure/ring-anti-forgery - CSRF
- https://github.com/r0man/inflections-clj - inflections for item(s)


## License

Copyright © 2021 Deon Moolman

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
