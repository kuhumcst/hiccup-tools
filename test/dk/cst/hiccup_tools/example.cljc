(ns dk.cst.hiccup-tools.example
  (:require [dk.cst.hiccup-tools.hiccup :as h]))

;; From https://html5example.com/
(def html5
  [:html {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:title "HTML5 Example Page"]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:meta {:name "description" :content "HTML5 Example Page"}]
    [:link {:rel "stylesheet" :href "/assets/css/stylesheet.css?v=1"}]]
   [:body
    [:header
     [:h1 "HTML5 Example Page"]]
    [:nav
     [:ul
      [:li [:a {:href ""} "Home"]]
      [:li [:a {:href ""} "About"]]
      [:li [:a {:href ""} "Contact"]]]]
    [:article
     [:h2 "This is a headline"]
     [:p "This paragraph is nested inside an article. It contains basic tags like" [:a {:href "#top"} "anchors"] ","
      [:strong "strong"] "," [:em "emphasis"] ", and" [:u "underline"] ".
        It provides" [:del "deleted text"] "as well, which often gets replaced with" [:ins "inserted"] "text."]
     [:h3 "This is subheadline for more content"]
     [:ul
      [:li "Unordered lists have basic styles"]
      [:li "They use the circle list style"
       [:ul
        [:li "Nested list item one"]
        [:li "Nested list item two"]]]
      [:li "Just one more list item"]]
     [:ol
      [:li "Ordered lists also have basic styles"]
      [:li "They use the decimal list style"
       [:ul
        [:li "Nested list item one"]
        [:li "Nested list item two"]]]
      [:li "Last list item"]]]
    [:aside
     [:h3 "This is some advanced content"]
     [:figure
      [:img {:data-src "/assets/images/image.png" :src "data:image/gif;base64,R0lGODlhAQABAIAAAMLCwgAAACH5BAAAAAAALAAAAAABAAEAAAICRAEAOw==" :width "360" :height "120" :alt "Description of the first image"}]
      [:figcaption "Caption for the first image"]]
     [:a {:href "#top"}
      [:figure
       [:img {:data-src "/assets/images/image2.png" :src "data:image/gif;base64,R0lGODlhAQABAIAAAAUEBAAAACwAAAAAAQABAAACAkQBADs=" :width "360" :height "80" :alt "Description of the second image"}]
       [:figcaption "Caption for the second image"]]]
     [:img {:alt "Description of a single image without a caption" :src "/assets/images/image.webp"}]
     [:p
      [:a.button {:href "#"} "Anchor button"] [:br]
      [:button "Button element"]]
     [:blockquote "“Infamous quote”"
      [:footer [:cite "- Attribution"]]]
     [:hr]
     [:details
      [:summary "Expandable title"]
      [:p "Revealed content"]]
     [:details
      [:summary "Another expandable title"]
      [:p "More revealed content"]]
     [:h4 "This is a code example"]
     [:pre [:code ".some-class {
  background-color: red;
}"]]]
    [:section
     [:h2 "This is another headline"]
     [:div.some-class "This is a boxed element."]
     [:div.some-class "This is yet another boxed element."]
     [:h3 "This is a table"]
     [:table
      [:thead
       [:tr
        [:th "Name"]
        [:th "Quantity"]
        [:th "Price"]]]
      [:tbody
       [:tr
        [:td "Cheese fondue"]
        [:td "1"]
        [:td "EUR 23"]]
       [:tr
        [:td "Pizza"]
        [:td "10"]
        [:td "EUR 10.000,00"]]
       [:tr
        [:td "Waffles"]
        [:td "1"]
        [:td "EUR 14,30"]]]]
     [:h3 "And this is a form"]
     [:form
      [:div.row
       [:label {:for "exampleSubject"} "Subject"]
       [:input#exampleSubject {:type "text" :placeholder "Your subject"}]]
      [:div.row
       [:label {:for "exampleMessage"} "Message"]
       [:textarea#exampleMessage {:placeholder "Your message"}]]
      [:div.row
       [:label
        [:input#exampleConfirmation {:type "checkbox"}] [:span "Confirmation"]]]
      [:input {:type "submit" :value "Submit"}]]]
    [:footer "Made with" [:a {:href "https://html5example.com"} "html5example.com"]]
    [:script {:src "/assets/js/scripts.js"}]]])

(comment
  ;; Update example text
  (spit "test/example.txt" (h/hiccup->text html5 h/html-conversion))
  #_.)
