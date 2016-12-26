/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vce;

/**
 *
 * @author yaitloutou
 */
public class VCExporterException extends Exception {

    public VCExporterException() {
    }

    public VCExporterException(String message) {
        super(message);
    }
}

class DomElementException extends VCExporterException {

    public DomElementException() {
        super("Dom element doesn't exists");
    }
}

class HtmlPageLoadingException extends VCExporterException {

    public HtmlPageLoadingException() {
        super("HtmlPage not loaded");
    }
}
