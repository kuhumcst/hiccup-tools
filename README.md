Hiccup tools
============
This library contains various functions for manipulating Hiccup data. The crown jewel of this effort is the function `split-hiccup` which can structurally split complex Hiccup trees into multiple tree structures.

My own use case was splitting an XML document into multiple pages at certain points so that it could be properly paginated within an HTML-based carousel widget without losing any of the structural information present in the original document.

## Background
I like to work with [Hiccup](https://github.com/weavejester/hiccup) data in Clojure projects. Hiccup has long since passed from being merely a popular library to being _the_ standard way of representing HTML/XML/SGML in Clojure. Many Clojure libraries either produce Hiccup or take it as input, e.g. Reagent. For this reason, it is quite convenient to be able to operate directly on Hiccup in both the frontend and backend.

One of the most flexible ways to do this is to treat the Hiccup tree as a [zipper](https://clojuredocs.org/clojure.zip/zipper) using `clojure.zip`. I have some [prior experience](https://github.com/kuhumcst/cuphic) attempting an entire DSL for both searching through and manipulating Hiccup data, but it was perhaps a bit too big of a mouthful for me (I don't have enough knowledge & resources to fully develop an extensive DSL like that) so I am not actively maintaining it now.

However, I still prefer Hiccup and associated zipper-based functions to many other ways of manipulating XML and HTML. This library is the place where I collect this kind of functionality.

Simon Gray,
Centre for Language Technology (University of Copenhagen)
