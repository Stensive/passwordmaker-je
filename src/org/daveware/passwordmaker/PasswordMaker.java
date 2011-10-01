/*
 * PasswordMaker Java Edition - One Password To Rule Them All
 * Copyright (C) 2011 Dave Marotti
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.daveware.passwordmaker;

import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class is used to generate passwords from a master password and an account.
 * @author Dave Marotti
 */
public class PasswordMaker {

    /**
     * Maps an array of characters to another character set.
     * 
     * This is the magic which allows an encrypted password to be mapped into a
     * a specific character set.
     * 
     * @param input The array of characters to map.
     * @param encoding The list of characters to map to.
     * @param trim Whether to trim leading zeros ... I think.
     * @return The mapped string.
     * @throws Exception On odd length!?
     */
    public SecureCharArray rstr2any(char[] input, String encoding, boolean trim) 
            throws Exception {
        int length = input.length;
        int divisor;
        int full_length;
        int[] dividend;
        int[] remainders;
        int remainders_count = 0;
        int dividend_length;
        int i, j;
        
        int outputPosition = 0;
        SecureCharArray output = new SecureCharArray();

        // can't handle odd lengths
        if ((length % 2) != 0) {
            return output;
        }

        divisor = encoding.length();
        dividend_length = (int) Math.ceil((double) length / 2.0);
        dividend = new int[dividend_length];
        for (i = 0; i < dividend_length; i++) {
            dividend[i] = (((int) input[i * 2]) << 8) | ((int) input[i * 2 + 1]);
        }

        full_length = (int) Math.ceil((double) length * 8 / (Math.log((double) encoding.length()) / Math.log((double) 2)));
        remainders = new int[full_length];

        if (trim) {
            while (dividend_length > 0) {
                // TODO: zero-out the quotient array each iteration at the end
                int[] quotient;
                int quotient_length = 0;
                int qCounter = 0;
                int x = 0;

                quotient = new int[dividend_length];
                for (i = 0; i < dividend_length; i++) {
                    int q;
                    x = (x << 16) + dividend[i];
                    q = (int) Math.floor((double) x / divisor);
                    x -= q * divisor;
                    if (quotient_length > 0 || q > 0) {
                        quotient[qCounter++] = q;
                        quotient_length++;
                    }
                }
                remainders[remainders_count++] = x;
                dividend_length = quotient_length;
                dividend = quotient;
            }
            full_length = remainders_count;
        } else {
            for (j = 0; j < full_length; j++) {
                int[] quotient;
                int quotient_length = 0;
                int qCounter = 0;
                int x = 0;

                quotient = new int[dividend_length];
                for (i = 0; i < dividend_length; i++) {
                    int q;
                    x = (x << 16) + dividend[i];
                    q = (int) Math.floor((double) x / divisor);
                    x -= q * divisor;
                    if (quotient_length > 0 || q > 0) {
                        quotient[qCounter++] = q;
                        quotient_length++;
                    }
                }
                remainders[j] = x;
                dividend_length = quotient_length;
                dividend = quotient;
            }
        }

        if(output.size() < full_length)
            output.resize(full_length, false);
        
        for (i = full_length - 1; i >= 0; i--) {
            output.setCharAt(outputPosition++, encoding.charAt(remainders[i]));
        }

        return output;
    }

    /**
     * Generates a hash of the master password with settings from the account.
     * @param masterPassword The password to use as a key for the various algorithms.
     * @param account The account with the specific settings for the hash.
     * @return A SecureCharArray with the hashed data.
     * @throws Exception if something bad happened.
     */
    public SecureCharArray makePassword(SecureCharArray masterPassword, Account account)
            throws Exception
    {
        LeetLevel leetLevel = account.getLeetLevel();
        //int count = 0;
        int length = account.getLength();
        SecureCharArray output = null;
        SecureCharArray data = null;

        try {
            if(account.getCharacterSet().length() < 2)
                throw new Exception("Account contains a character set that is too short");

            data = new SecureCharArray(account.getUrl() + account.getUsername() + account.getModifier());

            // Use leet before hashing
            if(account.getLeetType()==LeetType.BEFORE || account.getLeetType()==LeetType.BOTH) {
                LeetEncoder.leetConvert(leetLevel, masterPassword);
                LeetEncoder.leetConvert(leetLevel, data);
            }

            // Perform the actual hashing
            output = hashTheData(masterPassword, data, account);

            // Use leet after hashing
            if(account.getLeetType()==LeetType.AFTER || account.getLeetType()==LeetType.BOTH) {
                LeetEncoder.leetConvert(leetLevel, output);
            }

            // Apply the prefix
            if(account.getPrefix().length() > 0) {
                SecureCharArray prefix = new SecureCharArray(account.getPrefix());
                output.prepend(prefix);
                prefix.erase();
            }

            // Handle the suffix
            output.resize(length, true);
            if(account.getSuffix().length() > 0) {
                SecureCharArray suffix = new SecureCharArray(account.getSuffix());

                // If the suffix is larger than the entire password (not smart), then
                // just replace the output with a section of the suffix that fits
                if(length < suffix.size()) {
                    output.replace(suffix);
                    output.resize(length, true);
                }
                // Otherwise insert the prefix where it fits
                else {
                    output.resize(length - suffix.size(), true);
                    output.append(suffix);
                }

                suffix.erase();
            }
        }
        catch(Exception e) {
            if(output!=null)
                output.erase();
            throw e;
        }
        finally {
            // not really needed... but here for completeness
            if(data!=null)
                data.erase();
        }
        
        return output;
    }
    
    /**
     * Intermediate step of generating a password. Performs constant hashing until
     * the resulting hash is long enough.
     * 
     * @param masterPassword You should know by now.
     * @param data Not much has changed.
     * @param account A donut?
     * @return A suitable hash.
     * @throws Exception if we ran out of donuts.
     */
    private SecureCharArray hashTheData(SecureCharArray masterPassword, SecureCharArray data, Account account)
            throws Exception
    {
        SecureCharArray output                  = new SecureCharArray();
        SecureCharArray secureIteration         = new SecureCharArray();
        SecureCharArray intermediateOutput      = null;
        SecureCharArray interIntermediateOutput = null;
        int count   = 0;
        int length  = account.getLength();
        
        try {
            while(output.size() < length) {
                if(count==0) {
                    intermediateOutput = runAlgorithm(masterPassword, data, account);
                }
                else {
                    // add ye bit'o chaos
                    secureIteration.replace(masterPassword);
                    secureIteration.append(new SecureCharArray("\n"));
                    secureIteration.append(new SecureCharArray(Integer.toString(count)));
                    
                    interIntermediateOutput = runAlgorithm(secureIteration, data, account);
                    intermediateOutput.append(interIntermediateOutput);
                    interIntermediateOutput.erase();

                    secureIteration.erase();
                }
                output.append(intermediateOutput);
                intermediateOutput.erase();

                count++;
            }
        } catch(Exception e) {
            if(output!=null)
                output.erase();
            throw e;
        } finally {
            if(intermediateOutput!=null)
                intermediateOutput.erase();
            if(interIntermediateOutput!=null)
                interIntermediateOutput.erase();
            if(secureIteration!=null)
                secureIteration.erase();
        }
        
        return output;
    }
    
    /**
     * This performs the actual hashing. It obtains an instance of the hashing algorithm
     * and feeds in the necessary data.
     * 
     * @param masterPassword The master password to use as a key.
     * @param data The data to be hashed.
     * @param account The account with the hash settings to use.
     * @return A SecureCharArray of the hash.
     * @throws Exception if something bad happened.
     */
    private SecureCharArray runAlgorithm(SecureCharArray masterPassword, SecureCharArray data, Account account)
            throws Exception
    {
        SecureCharArray output = null;
        SecureCharArray digestChars = null;
        SecureByteArray masterPasswordBytes = null;
        SecureByteArray dataBytes = null;

        try {
            masterPasswordBytes = new SecureByteArray(masterPassword.getData());
            dataBytes = new SecureByteArray(data.getData());

            if (account.isHmac() == false) {
                dataBytes.prepend(masterPasswordBytes);
            }

            if (account.isHmac()) {
                Mac mac;
                String algoName = "HMAC" + account.getAlgorithm().getName();
                mac = Mac.getInstance(algoName, "BC");
                mac.init(new SecretKeySpec(masterPasswordBytes.getData(), algoName));
                mac.reset();
                mac.update(dataBytes.getData());
                digestChars = new SecureCharArray(mac.doFinal());
            } else {
                MessageDigest md = MessageDigest.getInstance(account.getAlgorithm().getName(), "BC");
                digestChars = new SecureCharArray(md.digest(dataBytes.getData()));
            }


            output = rstr2any(digestChars.getData(), account.getCharacterSet(), account.isTrim());
        } catch(Exception e) {
            if(output!=null)
                output.erase();
            throw e;
        } finally {
            if(masterPasswordBytes!=null)
                masterPasswordBytes.erase();
            if(dataBytes!=null)
                dataBytes.erase();
            if(digestChars!=null)
                digestChars.erase();
        }
        
        return output;
    }
}