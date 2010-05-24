/* Copyright 2009 British Broadcasting Corporation
 
Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.beans.bind;

import java.beans.PropertyEditorSupport;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

/**
 */
public class CustomDateEditor extends PropertyEditorSupport {

    protected static final Log logger = 
        LogFactory.getLog(CustomDateEditor.class);

    private List<DateFormat> dateFormat = new ArrayList<DateFormat>();

    private static final String TZ_FORMAT = "([+-][0-9]{1,2}):([0-9]{2})";
    private static final String TZ_REPL = "$1$2";
    
    private final boolean allowEmpty;

    /**
     */
    public CustomDateEditor(List<DateFormat> dateFormat, boolean allowEmpty) {
        this.dateFormat = dateFormat;
        this.allowEmpty = allowEmpty;
    }

    /**
     */
    public void setAsText(String text) throws IllegalArgumentException {
        if (this.allowEmpty && !StringUtils.hasText(text)) {
            // Treat empty String as null value.
            setValue(null);
        }
        else {
            boolean success = false;
            
            // FIXME: Hacked here to twist simple date format to
            // FIXME  accept colon separated timezones
            String fixed = text.replaceAll(TZ_FORMAT, TZ_REPL);

            for (DateFormat df : dateFormat) {
                ParsePosition pos = new ParsePosition(0);
                Date date = df.parse(fixed, pos);

                if (date != null 
                    && pos.getErrorIndex() == -1 
                    && pos.getIndex() == fixed.length()) {

                    setValue(date);
                    success = true;
                    break;
                }
            }

            if (!success) {
                throw new IllegalArgumentException(
                    "Could not parse date: unknown format [" + text + "]");
            }
        }
    }

    /**
     * Format the Date as String, using the specified DateFormat.
     */
    public String getAsText() {
        Date value = (Date) getValue();

        if (dateFormat.size() > 0) {
            return (value != null ? this.dateFormat.get(0).format(value) : "");
        }
        else {
            return "";
        }
    }

}
