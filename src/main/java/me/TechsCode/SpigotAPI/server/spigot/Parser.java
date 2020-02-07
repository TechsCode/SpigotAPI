package me.TechsCode.SpigotAPI.server.spigot;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import me.TechsCode.SpigotAPI.server.data.Entry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Parser {

    public static final String BASE = "https://www.spigotmc.org";

    private VirtualBrowser virtualBrowser;
    private String ownUserId;

    public Parser(String username, String password) throws AuthenticationException {
        virtualBrowser = new VirtualBrowser();

        HtmlPage page = virtualBrowser.request(Parser.BASE+"/login/login", HttpMethod.POST,
                new NameValuePair("cookieexists", "false"),
                new NameValuePair("login", username),
                new NameValuePair("password", password));

        this.ownUserId = getLoggedInUserId(page);

        if(ownUserId == null){
            throw new AuthenticationException(page);
        }
    }

    private String getLoggedInUserId(HtmlPage htmlPage){
        Document document = Jsoup.parse(htmlPage.asXml());

        try {
            Element link = document
                    .getElementsByClass("sidebar").first()
                    .getElementsByClass("visitorPanel").first()
                    .getElementsByClass("avatar").first();

            String userId = link.attr("href")
                    .replace("/", "")
                    .split("[.]")[1];

            return userId;
        } catch (Exception e){
            return null;
        }
    }

    public List<Entry> retrieveResources(){
        List<Entry> list = new ArrayList<>();
        Document resourcesPage = getSpigotPage("resources/authors/"+ownUserId);

        for(Element item : resourcesPage.getElementsByClass("resourceListItem")){
            Element resourceImg = item
                    .getElementsByClass("resourceIcon").first()
                    .getAllElements().first();

            Elements resourceDetails = item.getElementsByClass("resourceDetails").first().getAllElements();

            String resourceId = item.id().split("-")[1];
            String resourceName = item.getElementsByClass("title").first().getElementsByTag("a").first().text();
            String subTitle = item.getElementsByClass("tagLine").first().text();
            String category = resourceDetails.get(4).text();
            String costString = category.equalsIgnoreCase("premium") ? item.getElementsByClass("cost").first().text() : null;
            String icon = BASE+"/"+resourceImg.attr("src");
            String time = resourceDetails.get(2).text();

            Entry entry = new Entry();
            entry.set("resourceId", resourceId);
            entry.set("resourceName", resourceName);
            entry.set("subTitle", subTitle);
            entry.set("category", category);
            entry.setCost(costString);
            entry.set("icon", icon);
            entry.set("time", time);
            entry.set("unixTime", 0);
            list.add(entry);
        }

        return list;
    }

    public List<Entry> retrieveUpdates(List<Entry> resources){
        List<Entry> list = new ArrayList<>();

        for(Map.Entry<Element, Entry> pair : collectElementsOfSubPage(resources, "updates", "resourceUpdate").entrySet()){
            Element element = pair.getKey();
            Entry resource = pair.getValue();

            if(element.getElementsByClass("textHeading").isEmpty()){
                // If there are no updates, spigot redirects to the homepage. Since that div is also used there we have to block it.
                continue;
            }

            String resourceId = resource.getString("resourceId");
            String resourceName = resource.getString("resourceName");
            String updateId = element.id().split("-")[1];
            String title = element.getElementsByClass("textHeading").first().text();
            String description = element.getElementsByClass("messageText").first().text();
            String updatedAt = element.getElementsByClass("DateTime").first().text();

            Elements imageElements = element.select("img[data-url]");
            List<String> imagesEntry = imageElements.stream().map(elements -> elements.attr("data-url")).collect(Collectors.toList());

            Entry entry = new Entry();
            entry.set("updateId", updateId);
            entry.set("resourceId", resourceId);
            entry.set("resourceName", resourceName);
            entry.set("title", title);
            entry.set("description", description);
            entry.set("time", updatedAt);
            entry.set("unixTime", 0);
            entry.set("images", imagesEntry);
            list.add(entry);
        }

        return list;
    }

    public List<Entry> retrieveReviews(List<Entry> resources){
        List<Entry> list = new ArrayList<>();

        for(Map.Entry<Element, Entry> pair : collectElementsOfSubPage(resources, "reviews", "review").entrySet()) {
            Element element = pair.getKey();
            Entry resource = pair.getValue();

            String resourceId = resource.getString("resourceId");
            String resourceName = resource.getString("resourceName");
            String reviewId = element.id().split("-")[1];
            String text = element.getElementsByTag("blockquote").text().replace("<br>", "\n");
            int rating = Math.round(Float.valueOf(element.getElementsByClass("ratings").first().attr("title")));
            String username = element.attr("data-author");
            String userId = element.id().split("-")[2];
            String time = element.getElementsByClass("DateTime").first().text();

            Entry entry = new Entry();
            entry.set("reviewId", reviewId);
            entry.set("resourceId", resourceId);
            entry.set("resourceName", resourceName);
            entry.set("text", text);
            entry.set("rating", rating);
            entry.set("username", username);
            entry.set("userId", userId);
            entry.set("time", time);
            entry.set("unixTime", 0);
            list.add(entry);
        }

        return list;
    }

    public List<Entry> retrievePurchases(List<Entry> resources){
        List<Entry> list = new ArrayList<>();

        for(Map.Entry<Element, Entry> pair : collectElementsOfSubPage(resources, "buyers", "memberListItem").entrySet()) {
            Element element = pair.getKey();
            Entry resource = pair.getValue();

            Element link = element.getElementsByClass("StatusTooltip").first();

            Element costElement = element
                    .getElementsByClass("extra").last()
                    .getAllElements().last();

            String username = link.text();
            String userId = link.attr("href").replace("/", "").split("[.]")[1];
            String time = element.getElementsByClass("DateTime").first().text();
            String costString = costElement.tagName().equalsIgnoreCase("div") ? costElement.text().split(": ")[1] : null;

            Entry entry = new Entry();
            String resourceId = resource.getString("resourceId");
            String resourceName = resource.getString("resourceName");
            entry.set("purchaseId", resourceId + "-" + userId);
            entry.set("resourceId", resourceId);
            entry.set("resourceName", resourceName);
            entry.set("username", username);
            entry.set("userId", userId);
            entry.set("time", time);
            entry.set("unixTime", 0);
            entry.setCost(costString);
            list.add(entry);
        }

        return list;
    }

    private Map<Element, Entry> collectElementsOfSubPage(List<Entry> resources, String subPage, String uniqueClassName){
        Map<Element, Entry> map = new HashMap();

        for(Entry resource : resources){
            String resourceId = resource.getString("resourceId");
            String category = resource.getString("category");

            if(!category.equalsIgnoreCase("premium")){
                continue;
            }

            int pageAmount = 1;
            int currentPage = 1;

            while (currentPage <= pageAmount){
                Document document = getSpigotPage("resources/"+resourceId+"/"+subPage+"?page="+currentPage);

                for(Element element : document.getElementsByClass(uniqueClassName)){
                    map.put(element, resource);
                }

                pageAmount = getPageAmount(document);
                currentPage++;
            }
        }

        return map;
    }

    private int getPageAmount(Document document){
        if(document.getElementsByClass("pageNavHeader").isEmpty()){
            return 1;
        }

        Element pageNavHeader = document.getElementsByClass("pageNavHeader").first();
        String value = pageNavHeader.text().split(" of ")[1];
        return Integer.valueOf(value);
    }

    private Document getSpigotPage(String subPage){
        HtmlPage htmlPage = virtualBrowser.request(BASE+"/"+subPage, HttpMethod.GET);
        return Jsoup.parse(htmlPage.asXml());
    }


    public String getOwnUserId() {
        return ownUserId;
    }
}
