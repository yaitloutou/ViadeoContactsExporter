/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vce;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author yaitloutou
 */
public class ViadeoContact {

    private String id,name,headline = null;
    private List<String> emails,phones,chats,socials = null;
    ContactsContact contact = null;
    Where where = null;

    // Constructors    
    ViadeoContact() {
    }

    public ViadeoContact(String name, String headline, ContactsContact contact) {
        this.name = name;
        this.headline = headline;
        this.contact = contact;
    }

    public ViadeoContact(String id, String name, String headline, ContactsContact contact) {
        this.id = id;
        this.name = name;
        this.headline = headline;
        this.contact = contact;
    }

    // Getter & Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getPhones() {
        return phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public List<String> getChats() {
        return chats;
    }

    public void setChats(List<String> chats) {
        this.chats = chats;
    }

    public List<String> getSocials() {
        return socials;
    }

    public void setSocials(List<String> socials) {
        this.socials = socials;
    }

    public ContactsContact getContact() {
        return contact;
    }

    public void setContact(ContactsContact contact) {
        this.contact = contact;
    }

    public Where getWhere() {
        return where;
    }

    public void setWhere(Where where) {
        this.where = where;
    }

}

class Where {

    private String streetAddress;
    private String locality;
    private String postalCode;
    private String region;
    private String country;

    // Constructors
    public Where() {
    }

    Where(List<String> wheres) {
        if (wheres != null && wheres.size() > 3) {
            streetAddress = wheres.get(0);
            locality = wheres.get(1);
            postalCode = wheres.get(2);
            region = (wheres.size()>4) ? wheres.get(3): null;
            country = wheres.get(wheres.size()-1);
        }
    }

    // Getters & Setters
    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

}

class ContactsContact {

    private int contactNum;
    private boolean canSeeMyDirectContacts;
    private boolean showContacts;

    // Constructors
    public ContactsContact() {
    }

    public ContactsContact(int contactNum, boolean canSeeMyDirectContacts, boolean showContacts) {
        this.contactNum = contactNum;
        this.canSeeMyDirectContacts = canSeeMyDirectContacts;
        this.showContacts = showContacts;
    }

    // Getters & Setters
    public int getContactNum() {
        return contactNum;
    }

    public void setContactNum(int contactNum) {
        this.contactNum = contactNum;
    }

    public boolean isCanSeeMyDirectContacts() {
        return canSeeMyDirectContacts;
    }

    public void setCanSeeMyDirectContacts(boolean canSeeMyDirectContacts) {
        this.canSeeMyDirectContacts = canSeeMyDirectContacts;
    }

    public boolean isShowContacts() {
        return showContacts;
    }

    public void setShowContacts(boolean showContacts) {
        this.showContacts = showContacts;
    }

}
