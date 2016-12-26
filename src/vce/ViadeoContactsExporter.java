/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.stream.JsonGenerationException;
import org.apache.commons.logging.LogFactory;

// TODO: Fix-> it was the root culprit of `Exception in thread "main" java.lang.OutOfMemoryError: GC overhead limit exceeded`
/**
 *
 *
 * @author yaitloutou
 */
public class ViadeoContactsExporter {

    private static final String BASE_URL = "https://www.viadeo.com/";
    private static final String LOGIN_URL = "https://secure.viadeo.com/en/signin";
//    private static final String ADDRESSBOOK_URL = "https://www.viadeo.com/addressbook/"; 

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        scrapData();
    }

    private static void scrapData() {
        /* turn off annoying htmlunit warnings */
        LogFactory.getFactory().setAttribute(
                "org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog"
        );
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.INFO);

        try {
            System.out.println("Starting autoLogin on " + LOGIN_URL);
            // get the HtmlPage
            try (
                    // autoLogin
                    WebClient webClient = initializeWebClient(
                            autoLogin(LOGIN_URL, Config.LOGIN, Config.PASSWORD))) {
                // get the HtmlPage
                HtmlPage page = webClient.getPage(BASE_URL);
                // check if loged in
                HtmlAnchor loginLink = page.getFirstByXPath("//a[@href='https://secure.viadeo.com/en/signin']");
                boolean isLoggedIn = loginLink == null;
                if (isLoggedIn) {
                    System.out.println("Successfuly logged in !");

                    int totalCount = 0;
                    for (char c = Config.START_C; c <= Config.END_C; c++) {

                        final char lc = (char) (c + 32); // lower case
                        System.out.println(c);
                        String addressbookUrlC = getAddressbookUrlC(c); // Url of [c] contacts group 
                        // load [addressbook] page
//                            System.out.println(addressbookUrlC);
                        HtmlElement contactListUl = loadContactList(addressbookUrlC, webClient);
                        // System.out.println(contactListUl.asXml());
                        // get all the [contact] DomElements
                        List<DomElement> contacts = (List<DomElement>) contactListUl.getByXPath(getContactsGroupXPath(lc));
                        System.out.println(contacts.size());
                        String filename = generateFileName(c);
                        try (
                                FileWriter fw = new FileWriter(filename, true);
                                BufferedWriter bw = new BufferedWriter(fw, 32 * 1024);
                                PrintWriter out = new PrintWriter(bw, true)) {
                            // for each [contact] ...
                            int count = 0, start = Config.START_I, end = Config.END_I;
                            for (DomElement contact : contacts) {
                                ++count;
//                            System.out.println(contact.asXml());
                                if (count > start) {
                                    ViadeoContact vc;
                                    vc = parseContact(contact);
//                                    System.out.println(getAsJsonString(vc));
                                    String contactDetailsUrl = getContactDetailsUrl(addressbookUrlC, vc.getId());
                                    DomElement contactDetailsDom = loadContactDetails(contactDetailsUrl, webClient);
//                                System.out.println(contactDetailsDom.asXml());
                                    vc = parseContactDetails(vc, contactDetailsDom);
//                                    System.out.println(getAsJsonString(vc, true));
                                    if (vc != null) {
                                        out.println(getAsJsonString(vc));
//                                        System.out.println(count);
                                        ++totalCount;
                                        vc = null;
                                    }
                                }
                                if (end > 0 && count > end)
                                    break;

                                System.gc();
                            }
                        } //  try-with-resources takes care of the closes -> http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
                        System.out.println(
                                new StringBuilder()
                                        .append(totalCount)
                                        .append(" contact(s) scrapped, and saved at: ")
                                        .append(filename)
                        );
                    }

                } else {
                    System.err.println("Email address and/or password invalid");
                }
                webClient.close();
            }

        } catch (IOException | FailingHttpStatusCodeException ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String generateFileName(char c) {
        // create contact.json file
        SimpleDateFormat reversedDateFormat = new SimpleDateFormat("yyMMddhhmmss");
        String reversedDate = reversedDateFormat.format(new Date());
        return new StringBuilder("./contacts/")
                .append(reversedDate)
                .append("_")
                .append(c)
                .append("_contacts.json").toString();
    }

    private static Set<Cookie> autoLogin(String loginUrl, String login, String password) {
        Set<Cookie> cookies = null;
        WebClient webClient = initializeWebClient(null);

        HtmlPage page;
        try {
            WebRequest requestSettings = initializeRequest(loginUrl);

            page = webClient.getPage(requestSettings);

            HtmlInput inputPassword = page.getFirstByXPath("//input[@type='password']");
            //The first preceding input that is not hidden
            HtmlInput inputLogin = inputPassword.getFirstByXPath(".//preceding::input[not(@type='hidden')]");

            inputLogin.setValueAttribute(login);
            inputPassword.setValueAttribute(password);

            //get the enclosing form
            HtmlForm loginForm = inputPassword.getEnclosingForm();

            //submit the form
            page = webClient.getPage(loginForm.getWebRequest(null));

            // Set Cookiies
            cookies = webClient.getCookieManager().getCookies();

        } catch (IOException | FailingHttpStatusCodeException ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }

        //returns the cookie filled client :)
        return cookies;
    }

    private static WebRequest initializeRequest(String... args) throws MalformedURLException {
        URL url = new URL(args[0]);

        WebRequest requestSettings;
        requestSettings = new WebRequest(url, HttpMethod.GET);
        requestSettings.setAdditionalHeader("Accept", "*/*");
        requestSettings.setAdditionalHeader("Content-Type", "application/x-javascript");
        requestSettings.setAdditionalHeader("Referer", "REFURLHERE");
        requestSettings.setAdditionalHeader("Accept-Language", "en-US,en;q=0.8");
        requestSettings.setAdditionalHeader("Accept-Encoding", "gzip,deflate,sdch");
        requestSettings.setAdditionalHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        requestSettings.setAdditionalHeader("X-Requested-With", "XMLHttpRequest");
        requestSettings.setAdditionalHeader("Cache-Control", "no-cache");
        requestSettings.setAdditionalHeader("Pragma", "no-cache");
        requestSettings.setAdditionalHeader("Origin", "www.viadeo.com");

//        requestSettings.setRequestBody("REQUESTBODY");
        return requestSettings;
    }

    private static WebClient initializeWebClient(Set<Cookie> cookies, Boolean... args) {
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_45);

        boolean[] flag = new boolean[4]; // initialize all the flag with false
        for (int i = 0; i < args.length; i++) // then get the true ones 
            flag[i] = args[i];

        webClient.getOptions().setJavaScriptEnabled(flag[0]);
        webClient.getOptions().setCssEnabled(flag[1]);
        webClient.getOptions().setThrowExceptionOnScriptError(flag[2]);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(flag[3]);

        if (cookies != null) {
            Iterator<Cookie> i = cookies.iterator();
            while (i.hasNext()) {
                webClient.getCookieManager().addCookie(i.next());
            }
        }

        return webClient;
    }

    private static HtmlElement loadContactList(String addressbookUrl, WebClient webClient) {

        HtmlPage addressbookPage = null;
        HtmlElement contactList = null;
        try {
            webClient.getOptions().setJavaScriptEnabled(true);

            WebRequest webRequest = initializeRequest(addressbookUrl);
            addressbookPage = webClient.getPage(webRequest); // the peer may reset the connection, while you're waiting for the page -> bad cookies -> no access -> no page & an IOException

            webClient.waitForBackgroundJavaScript(10000);
            webClient.waitForBackgroundJavaScriptStartingBefore(10000);

            contactList = addressbookPage.getFirstByXPath("//div[@id='contactList']/ul");
            if (addressbookPage == null || contactList == null) {
                throw new HtmlPageLoadingException();
            }
//            System.out.println(contactListDiv.asXml());
            for (int i = 0; i < 10; i++) {
                HtmlElement firstContactItemDiv = contactList.getFirstByXPath("//div[@class='contactItem ']"); //there is a space in the @class name!!
//                System.out.println(firstContactItemDiv);
                if (firstContactItemDiv != null)
                    break;
                System.out.println("Loading... " + i);
                synchronized (contactList) {
                    contactList.wait(10000);
                }
            }
        } catch (FailingHttpStatusCodeException
                | InterruptedException
                | HtmlPageLoadingException
                | IOException ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return contactList;
    }

    private static DomElement loadContactDetails(String contactDetailsUrl, WebClient webClient) {

        HtmlPage contactDetailsPage = null;
        DomElement contactDetailsDom = null;
        try { // TODO: reduce load time
            contactDetailsPage = webClient.getPage(contactDetailsUrl);
            webClient.waitForBackgroundJavaScript(10000);
            webClient.waitForBackgroundJavaScriptStartingBefore(10000);

//            System.out.println(contactDetailsPage.asXml());
            for (int i = 0; i < 5; i++) {
                HtmlElement contactDetailsHeaderDiv = contactDetailsPage.getFirstByXPath("//div[@class='ellipsis']");
                boolean isLoaded = !contactDetailsHeaderDiv.asText().equals("Contact");
                if (isLoaded)
                    break;
                synchronized (contactDetailsPage) {
                    contactDetailsPage.wait(10000);
                }
                System.out.println("Loading... " + i);
            }
            contactDetailsDom = contactDetailsPage.getFirstByXPath("//div[@class='gu gu-last pbs']");
            contactDetailsPage = null;

        } catch (IOException | FailingHttpStatusCodeException | InterruptedException //| HtmlPageLoadingException 
                ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return contactDetailsDom;
    }

    private static String getAddressbookUrlC(char c) {
        return "https://www.viadeo.com/addressbook/#ck=&tab=&s=%7B%22ty%22%3A%22contact%22%2C%22l%22%3A%22" + c + "%22%7D";
    }

    private static String getContactsGroupXPath(char lc) {
        // to get the HTML Xpath, download and install firefox plugin Xpather from
        // http://jassage.com/xpather-1.4.5b.xpi
        // or use: getCanonicalXPath
        return "//div[@class='contactItem ']/li[@name='" + lc + "']";
    }

    private static String getContactDetailsUrl(String addressbookUrlC, String id) {
        return addressbookUrlC
                .replace("#ck=", "#ck=" + id + "-contact")
                .replace("&tab=", "&tab=resume");
    }

    private static List<Object> initializeContactBasicData(DomElement contact) {
        List<Object> data = null;
        try {
            // get DomElements
            DomNodeList<HtmlElement> spans = contact.getElementsByTagName("span");
            if (spans == null)
                throw new DomElementException();
            // assigne values to attribute
            String id = contact.getAttribute("rel");
            id = id.substring(0, id.indexOf("-"));
            data = new ArrayList<>(6);
            // 0: id
            data.add(id);
            // 1: name
            data.add(spans.get(3).asText()); //name
            // 2: headline
            data.add(spans.get(4).asText().isEmpty() ? null : spans.get(4).asText());
            // 4: contactNum
            data.add(Integer.parseInt(spans.get(1).asText()));
            // 5: canSeeMyDirectContacts
            data.add(spans.get(1).getAttribute("class").contains("canSeeMyDirectContacts"));
            // 6: showContacts
            data.add(spans.get(1).getAttribute("class").contains("showContacts"));

        } catch (DomElementException ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }

    private static List<List<String>> initializeContactDetailsData(DomElement contactDetailsDom) {
        List<List<String>> data = null;
        try {
            // get DomElements & assigne values to local attribute
            DomNodeList<HtmlElement> h4s = contactDetailsDom.getElementsByTagName("h4");
            if (h4s == null)
                throw new DomElementException();
            HtmlElement wheresElement = h4s.get(0);

            data = new ArrayList<>(5);
            data.add(initializeContactDetailsList(wheresElement));
            wheresElement = null;

            DomNodeList<HtmlElement> uls = contactDetailsDom.getElementsByTagName("ul");
            if (uls == null)
                throw new DomElementException();
            for (HtmlElement ul : uls) {
                String ulClass = ul.getAttribute("class");
                data.add(
                        ulClass.contains("email-list") ? initializeContactDetailsList(ul) : null);
                data.add(
                        ulClass.contains("main-phone") ? initializeContactDetailsList(ul) : null);
                data.add(
                        ulClass.contains("chat") ? initializeContactDetailsList(ul) : null);
                data.add(
                        ulClass.contains("social") ? initializeContactDetailsList(ul) : null);
            }
            uls = null;

        } catch (DomElementException ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }

    private static ViadeoContact parseContact(DomElement contact) {
        ViadeoContact vc = null;
        try {
            // initialize Contact Basic Data
            List<Object> data = initializeContactBasicData(contact);
            if (data.isEmpty() || data.size() < 5)
                throw new VCExporterException("conatct basic data lits is empty or not complete");
            // instantiate Objects
            vc = new ViadeoContact(
                    //id
                    (String) data.get(0),
                    //name
                    (String) data.get(1),
                    //headline
                    (String) data.get(2),
                    new ContactsContact(
                            //contactNum
                            (int) data.get(3),
                            //canSeeMyDirectContacts
                            (boolean) data.get(4),
                            //showContacts)
                            (boolean) data.get(5)
                    )
            );
        } catch (VCExporterException ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return vc;
    }

    private static ViadeoContact parseContactDetails(ViadeoContact vc, DomElement contactDetailsDom) {

        // get DomElements & assigne values to local attribute
        List<List<String>> data = initializeContactDetailsData(contactDetailsDom);
        // set [conatact] attributes
        vc.setWhere(new Where(data.get(0)));
        vc.setEmails(data.get(1));
        vc.setPhones(data.get(2));
        vc.setSocials(data.get(3));
        vc.setChats(data.get(4));

//        System.out.println(getAsJsonString(vc));
        return vc;
    }

    private static List<String> initializeContactDetailsList(HtmlElement element) {
//        System.out.println(">initializeList");
        List<String> list = null;
        if (element != null) {
            list = new ArrayList<>();
//            System.out.println(element.asXml());
            Iterable<DomElement> childElements = element.getChildElements();
            for (Iterator<DomElement> it = childElements.iterator(); it.hasNext();) {
                DomElement childElement = it.next();
                String childTagName = childElement.getTagName();
                boolean isListElement = "li".equals(childTagName) || "span".equals(childTagName);
                if (isListElement)
//                System.out.println("ListElement");
//                System.out.println(childElement.asXml());
                    if (isListElement) {
                        String text = childElement.asText();
                        text = text.isEmpty() ? null : childElement.asText();
                        list.add(text);
                    }
//                System.out.println("asText:\t"+childElement.asText());
            }
        }
//        System.out.println(list+"\n---------------");
        return list.isEmpty() ? null : list;
    }

    private static String getAsJsonString(Object obj) {
        return getAsJsonString(obj, false);
    }

    private static String getAsJsonString(Object obj, boolean isPretty) {
        String jsonInString = "{}";
        ObjectMapper mapper = new ObjectMapper();
        try { // Convert object to JSON string and pretty print
            jsonInString = isPretty ? 
                    mapper.writeValueAsString(obj) : mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonGenerationException | IOException ex) {
            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return jsonInString;
    }

//    private static void saveAsJsonFile(Object obj) {
//        ObjectMapper mapper = new ObjectMapper();
//        try {
//            File file = new File("contacts.json");
//            // Convert object to JSON string and save into a file directly
//            mapper.writeValue(file, obj);
//            System.out.println("JSON file saved at \n" + file.getAbsolutePath());
//        } catch (JsonGenerationException | IOException ex) {
//            Logger.getLogger(ViadeoContactsExporter.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }

}
