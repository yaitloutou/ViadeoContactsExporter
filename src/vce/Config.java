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
public final class Config {

    public static final String LOGIN = "my.address@email.com";
    public static final String PASSWORD = "secret";
    
    // counters to get partial data
    static final char START_C = 'A';
    static final char END_C = 'Z';
    static final int START_I = 0;
    static final int END_I = -1; // -1 -> go 'till end
    
    // load time
    // TODO
    
    // private constructor
    private Config() {
        //this prevents even the native class from calling this constructor
        throw new AssertionError();
    }
}
