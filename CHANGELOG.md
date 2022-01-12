### [0.0.15](https://github.com/theiceshelf/firn/compare/v0.0.14...v0.0.15) (2022-01-11)

Everything changes. Firn was re-written. This will break your old version of Firn and using it will require that you re-setup your site. Sorry!

#### ⚠ BREAKING CHANGES

* Most of the old API is completely irrelevant.
* No longer using Hiccup for templating -> using Tera.
* Folding of headlines removed
* Rendering of properties removed
* #+FIRN_UNDER removed (for now)
* Tagging is less opinionated but more powerful.
  

#### Features/Added Things

* Scss/sass compilation is built into the site
* Compilation times should be faster (or else I did something wrong)


### [0.0.14](https://github.com/theiceshelf/firn/compare/ec3b6243509f7ad63f72d25e9f5ffa53436beba8...v0.0.14) (2021-05-24)

#### ⚠ BREAKING CHANGES

* Using a sub-part of the generated HTML as the RSS description (#90)
  * see https://firn.theiceshelf.com/rss
  * now, user's can specify what part of their content shows up in their RSS.
* render fns should return nil if no content.
  * previously, some render functions returned empty lists if no content was found to render.
  * we should rather return nil for better user experience with rendering layouts.

#### Features

- Add fixed width renderer ([1933082a](https://github.com/theiceshelf/firn/commit/1933082afce607991d7bcaf1539ce84f1cf99a54)))
- Enable users to define custom todos for their site ([102](https://github.com/theiceshelf/firn/pull/102))

### [0.0.13](https://github.com/theiceshelf/firn/compare/v0.0.12...v0.0.13) (2021-02-24)


#### ⚠ BREAKING CHANGES

* Using a sub-part of the generated HTML as the RSS description (#90)

#### Features

* Adjust parser CLI for compatibility with Linux dev ([#75](https://github.com/theiceshelf/firn/issues/75)) ([53c0b99](https://github.com/theiceshelf/firn/commit/53c0b9934757ba4c26ba87ce4a5905bd5157b9dc))
* Adjust parser CLI for compatibility with Linux dev ([#75](https://github.com/theiceshelf/firn/issues/75)) ([cbf774a](https://github.com/theiceshelf/firn/commit/cbf774a1b07f485d97512be8f2fcb7e9a610d54d))


#### Bug Fixes

* [#66](https://github.com/theiceshelf/firn/issues/66) - .org in firn directory name. ([c820a7f](https://github.com/theiceshelf/firn/commit/c820a7fd732c35a503757830e8122903287f54b4))
* changelog cmd should use `npx` ([13e3af2](https://github.com/theiceshelf/firn/commit/13e3af227682e9a5ad8b70db6862882ef16e0f9d))
* enable lowercase #+title frontmatter. ([7053b5b](https://github.com/theiceshelf/firn/commit/7053b5bf80b3336e2175e4bbd2424ebfc61d4186))
* Using a sub-part of the generated HTML as the RSS description ([#90](https://github.com/theiceshelf/firn/issues/90)) ([5c51e99](https://github.com/theiceshelf/firn/commit/5c51e99f101d7a52c3961a20d2681d210d43153d))

## [](https://github.com/theiceshelf/firn/compare/v0.0.12...v) (2020-11-05)

### [0.0.12](https://github.com/theiceshelf/firn/compare/v0.0.11...v0.0.12) (2020-11-05)


#### Features

* Add server repl. ([#59](https://github.com/theiceshelf/firn/issues/59)) ([10c0af9](https://github.com/theiceshelf/firn/commit/10c0af96b29de5159de298ab27460456e8669ce4))
* Enable println and prn in layouts/partials. ([2a487ed](https://github.com/theiceshelf/firn/commit/2a487ed8bddf5d2ea73dfd00cad2b6cd2b793ec2))


#### Bug Fixes

* date-parser was only making date-created. ([645fdbc](https://github.com/theiceshelf/firn/commit/645fdbceefb9e9b8ff646aa869ffd05c3a459f92))

## [](https://github.com/theiceshelf/firn/compare/v0.0.11...v) (2020-10-25)

### [0.0.11](https://github.com/theiceshelf/firn/compare/v0.0.9...v0.0.11) (2020-10-25)

#### ⚠ BREAKING CHANGES

* render fns should return nil if no content.
  - a render function such as `(render :backlinks)` would return an empty [:ul] element rather than nil if there were no backlinks.
  - this made conditionally rendering sections difficult.
* Firn file-based tag renderer and docs.
  - this release adds a render function: `(render :firn-file-tags) (see: "firn tags"` in documentation).

#### Features

* Add :depth filtering to render :site-map ([c44a521](https://github.com/theiceshelf/firn/commit/c44a521ac71e460621f72cbedde66624864e5f3a))
* exclude tags in (render :firn-tags) ([83e226a](https://github.com/theiceshelf/firn/commit/83e226a6414c58d876e226bd40c889cfb213c325))


#### Bug Fixes

* add site title, author, and description to layouts ([#41](https://github.com/theiceshelf/firn/issues/41)) ([a2a6b40](https://github.com/theiceshelf/firn/commit/a2a6b4037a30daab27822a27371e3f7f3ad50dd2))
* declare primary color css property for starter ([#50](https://github.com/theiceshelf/firn/issues/50)) ([3a9c10f](https://github.com/theiceshelf/firn/commit/3a9c10f2bff0120909442821fd06e83908024a21))
* Don't exit on files without frontmatter. ([bbd715c](https://github.com/theiceshelf/firn/commit/bbd715c4ccc2ef41ec3c569459edd6072a4d9276))
* Fix head-partial  [#54](https://github.com/theiceshelf/firn/issues/54) ([ba56116](https://github.com/theiceshelf/firn/commit/ba5611638bb1de08367fbd7b0dea80b0e2836757))
* include `jpeg` as possible image type. ([9bf21ec](https://github.com/theiceshelf/firn/commit/9bf21ec578d5ac09bbbd37736977aafd4247e84e))
* Enable: linking between parent directories from child ([05160b2](https://github.com/theiceshelf/firn/commit/05160b2991fa4cee4f48a1b669db42bad688cf27))
* remove firn.org circular dependency ([#42](https://github.com/theiceshelf/firn/issues/42)) ([6cf34f7](https://github.com/theiceshelf/firn/commit/6cf34f78d2f672a09d90722f1e48ae3596875619))
* remove opinionated padding on folded headlines. ([8d9318f](https://github.com/theiceshelf/firn/commit/8d9318feea974bebf017decc15c9ba685638396e))
* render fns should return nil if no content. ([5c370be](https://github.com/theiceshelf/firn/commit/5c370bedebc1444a8797d057b61fa18ebc77fc35))
* render links to private files as plaintext. ([0f05fc4](https://github.com/theiceshelf/firn/commit/0f05fc4baf6b3d391f569aef6db3e3a5d1ceff5b))
* site-url change for self-host. ([4892808](https://github.com/theiceshelf/firn/commit/4892808af78f60bb142e387aa364fa03f8110e43))


#### Code Refactoring

* Firn file-based tag renderer and docs. ([91dec43](https://github.com/theiceshelf/firn/commit/91dec43bf068201f2fea6c338f92d47ecd8a836a))

### [0.0.10](https://github.com/theiceshelf/firn/compare/v0.0.9...v0.0.10) (2020-10-20)


#### ⚠ BREAKING CHANGES

* Firn file-based tag renderer and docs. 
  - There is a new property in `config.edn` - the key `:firn-tags-path`, which is required if you want to specify what page is linked to when clicking on a firn-file-tag.

#### Bug Fixes

* add site title, author, and description to layouts ([#41](https://github.com/theiceshelf/firn/issues/41)) ([a2a6b40](https://github.com/theiceshelf/firn/commit/a2a6b4037a30daab27822a27371e3f7f3ad50dd2))
* declare primary color css property for starter ([#50](https://github.com/theiceshelf/firn/issues/50)) ([f2e7552](https://github.com/theiceshelf/firn/commit/f2e75520e4a793561366d0c59fd44ac672e6c185))
* declare primary color css property for starter ([#50](https://github.com/theiceshelf/firn/issues/50)) ([3a9c10f](https://github.com/theiceshelf/firn/commit/3a9c10f2bff0120909442821fd06e83908024a21))
* Don't exit on files without frontmatter. ([bbd715c](https://github.com/theiceshelf/firn/commit/bbd715c4ccc2ef41ec3c569459edd6072a4d9276))
* include `jpeg` as possible image type. ([9bf21ec](https://github.com/theiceshelf/firn/commit/9bf21ec578d5ac09bbbd37736977aafd4247e84e))
* remove firn.org circular dependency ([#42](https://github.com/theiceshelf/firn/issues/42)) ([6cf34f7](https://github.com/theiceshelf/firn/commit/6cf34f78d2f672a09d90722f1e48ae3596875619))
* remove opinionated padding on folded headlines. ([1b37293](https://github.com/theiceshelf/firn/commit/1b37293c16035019d684172d618c996421cb67cf))
* render links to private files as plaintext. ([0f05fc4](https://github.com/theiceshelf/firn/commit/0f05fc4baf6b3d391f569aef6db3e3a5d1ceff5b))
* site-url change for self-host. ([4892808](https://github.com/theiceshelf/firn/commit/4892808af78f60bb142e387aa364fa03f8110e43))


#### Code Refactoring

* Firn file-based tag renderer and docs. ([91dec43](https://github.com/theiceshelf/firn/commit/91dec43bf068201f2fea6c338f92d47ecd8a836a))

### [0.0.9](https://github.com/theiceshelf/firn/compare/v0.0.8...v0.0.9) (2020-09-25)


#### Bug Fixes

* site-map root links ([#39](https://github.com/theiceshelf/firn/issues/39)) ([550f163](https://github.com/theiceshelf/firn/commit/550f1634851a2057fd65419ecbaedea31436b203))

### [0.0.8](https://github.com/theiceshelf/firn/compare/v0.0.7...v0.0.8) (2020-07-29)


#### Features

* Enable excluding files from site-map. ([bfaefc4](https://github.com/theiceshelf/firn/commit/bfaefc4c907442ef15e6c2fb5f316e903637863c))
* remove unused assets on build cleanup. ([#35](https://github.com/theiceshelf/firn/issues/35)) ([c519446](https://github.com/theiceshelf/firn/commit/c5194466c5e324ab876887f941fe303b5b718c64))

### [0.0.7](https://github.com/theiceshelf/firn/compare/v0.0.6...v0.0.7) (2020-07-05)


#### Features

* add tags to headlines + tag page. ([#26](https://github.com/theiceshelf/firn/issues/26)) ([aa5f757](https://github.com/theiceshelf/firn/commit/aa5f75742241a5af6e6a71463bf06e43eb2e210e))

### [0.0.6](https://github.com/theiceshelf/firn/compare/v0.0.4...v0.0.6) (2020-06-25)


#### Features

* Table of contents. ([#18](https://github.com/theiceshelf/firn/issues/18)) ([eb381c4](https://github.com/theiceshelf/firn/commit/eb381c4f472db6ab2e6c0b53b21fb6c2e8945e4a))


#### Bug Fixes

* clean-anchor needed to replace slashes. ([#16](https://github.com/theiceshelf/firn/issues/16)) ([18e5489](https://github.com/theiceshelf/firn/commit/18e5489b7998f4c026e7735e474a35f27a632e8a))
* exponential use of swap! + concat 😅. ([333908c](https://github.com/theiceshelf/firn/commit/333908c4e0dab1afdfd4d9e9b4cbecd129ccc685))

### [0.0.4](https://github.com/theiceshelf/firn/compare/v0.0.3...v0.0.4) (2020-05-29)


#### Features

* add clean anchor and internal link handler. ([#13](https://github.com/theiceshelf/firn/issues/13)) ([17a7cbb](https://github.com/theiceshelf/firn/commit/17a7cbb998009b6bbf7dddf4cffbca5b0af49f4b))
* Rss  ([#14](https://github.com/theiceshelf/firn/issues/14)) ([f2a5e87](https://github.com/theiceshelf/firn/commit/f2a5e874b5fc460aaa5478bf33fd2778f123358a)), closes [#13](https://github.com/theiceshelf/firn/issues/13)

### [0.0.3](https://github.com/theiceshelf/firn/compare/v0.0.2...v0.0.3) (2020-05-26)


#### Features

* Enable footnotes ([7b5cd15](https://github.com/theiceshelf/firn/commit/7b5cd152f822aba86a576410c89688a52619dcb5))
* fix get-cwd and get compiled linux working. ([a6be1c3](https://github.com/theiceshelf/firn/commit/a6be1c3d7bc68c560a72169c0fcd0886599b8c39))
* logbook stats ([#11](https://github.com/theiceshelf/firn/issues/11)) ([81b6486](https://github.com/theiceshelf/firn/commit/81b648696b5a7886875bdec8a1b0316eb09341e5))


#### Bug Fixes

* A little bit of reflection. ([07245b4](https://github.com/theiceshelf/firn/commit/07245b45f7fd5f5b73def8338cd2d0174e22ace1))
* better parse-int func. ([6c247cb](https://github.com/theiceshelf/firn/commit/6c247cbb4478f370bd9585b97b9aed62ab9425c3))
* build issues. ([6220c56](https://github.com/theiceshelf/firn/commit/6220c5608d397fde9ba3f0588e47528541abe42c))
* bump. ([f630300](https://github.com/theiceshelf/firn/commit/f63030007a0dc8e63002aee1d159a6bd3f6577d2))
* cd into clojure dir. ([78e3d98](https://github.com/theiceshelf/firn/commit/78e3d981978a463f69eb90a3461fc6b437c2fc68))
* check index exists before serving at root. ([ffec95c](https://github.com/theiceshelf/firn/commit/ffec95c5f82c5e57c86bf9d49f43b7dce6d8a1ec))
* CI Push to find reflection.json. ([8db7980](https://github.com/theiceshelf/firn/commit/8db7980a65459dc8f55021c914a4ba27944923ff))
* convert _ to hyphen in partial/layout names. ([e544f97](https://github.com/theiceshelf/firn/commit/e544f97acb4b9273f5b85250c6de2f27922a636f))
* Correct boolean test on print-err! ([3d7eefd](https://github.com/theiceshelf/firn/commit/3d7eefda88c5bc68c57831685d877e32052e950b))
* direct path to parser. ([2990080](https://github.com/theiceshelf/firn/commit/2990080793e1a0061ce1bdd81b4a9280443a5f2f))
* Don't invoke server! ([0b2549a](https://github.com/theiceshelf/firn/commit/0b2549a7886c5b7da6f7f56d86ff57fc23c79e81))
* don't start server on compile. ([dbc63b4](https://github.com/theiceshelf/firn/commit/dbc63b450aae8a9e69c3c37dc39d96ab23db5327))
* external links open in a new tab. ([9921765](https://github.com/theiceshelf/firn/commit/99217658b4e8dc9dc67fbff8451b4e75269ed652))
* figure/figcaption cannot be inside <p> ([5731e2f](https://github.com/theiceshelf/firn/commit/5731e2fe9a9e94f3029b6057302b85c2e83920c0))
* get resources working ([69792f6](https://github.com/theiceshelf/firn/commit/69792f639dcaa518e48504050705bf5b285a2a0a))
* handler, config merging, in memory serving. ([87713a9](https://github.com/theiceshelf/firn/commit/87713a90d6cc08848f03b733a0e99f9d73c3be18))
* HTTP SERVER works. Fix linking in index. ([bd777a7](https://github.com/theiceshelf/firn/commit/bd777a7754ef4e99ed464d98d29927fedd44702c))
* implement system exist on print-err! ([5aba693](https://github.com/theiceshelf/firn/commit/5aba69320dec322d8d31472dbfc3201ae54e44e5))
* is-private? now respects paths in ignored-dirs. ([d8eaf55](https://github.com/theiceshelf/firn/commit/d8eaf55457559ce51fbb8b050fac2ded4df24739))
* logbook throws errors on inability to parse ([7aafdb2](https://github.com/theiceshelf/firn/commit/7aafdb22c86b15ad0a1031b2c456a1e9a6a2156a))
* Make `find-files-by-ext` use print-err! ([167e654](https://github.com/theiceshelf/firn/commit/167e6541af336d3f31bf41c5cc57b800e5c851d4))
* make parser path not relative. ([6e659ab](https://github.com/theiceshelf/firn/commit/6e659ab12162445fba9bccf93e7a29b50073360c))
* misplaced comment. ([d287521](https://github.com/theiceshelf/firn/commit/d28752127e1d82875073a2d88989132cac3b635c))
* Nest code element inside pre in src->htlm. ([d70c9dc](https://github.com/theiceshelf/firn/commit/d70c9dc2aff77570312bfbe3d535de3be9f824f6))
* new-site can now run in cwd ([96c50bb](https://github.com/theiceshelf/firn/commit/96c50bbdefc986350b31364e08b18c1442071660))
* only copy assets if folder doesn't exist ([ec5d679](https://github.com/theiceshelf/firn/commit/ec5d679f477d1c4ad41e352eb4899c2cd7450d38))
* remove null compile flag. Add :file to layout map. ([8e9bdd9](https://github.com/theiceshelf/firn/commit/8e9bdd96158de1b05003d605fac8dbee4bd93652))
* remove println. ([ebbe2d8](https://github.com/theiceshelf/firn/commit/ebbe2d804b0271ee1ac46722aea39172ba90d3e1))
* show sitemap in index. Cleanup more TODOs. ([4291692](https://github.com/theiceshelf/firn/commit/4291692af93658a5bfe511bf05ce7cdfc7a1b33c))
* trimline for linux? ([8e70165](https://github.com/theiceshelf/firn/commit/8e7016593d84b14c2ddc99eda09d3a5dc72751d3))
* try building on mac for the bin/parser ([75254b1](https://github.com/theiceshelf/firn/commit/75254b13a993d161744e07340228eaaedf2c7e3e))
* try returning promise to stop sys/exit of climatic. ([0ba10da](https://github.com/theiceshelf/firn/commit/0ba10daacd81efb7b70d5b6a84d518b3a95b441f))
* typos ([4a018ba](https://github.com/theiceshelf/firn/commit/4a018ba526faa6cf10e365ca8e9454f73b6cf282))
* vendor parser + move it to _firn/bin in setup ([b9259f7](https://github.com/theiceshelf/firn/commit/b9259f7824d01a8288b16d6295ac4996f67b2fe0))
* when -> when-not in dev? ([e2dd4f8](https://github.com/theiceshelf/firn/commit/e2dd4f81dc884fd96ceee298e6048f10b13f49b7))

