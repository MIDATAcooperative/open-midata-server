/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils;

import models.User;
import utils.context.AccessContext;
import utils.exceptions.AppException;

public class TestAccountTools {

    public static String testCustomerFromName(String name) {
        String parts[] = name.split("\\-");
        if (parts.length == 3) {
            if (!parts[2].equals("test")) return null;
            return parts[0];
        }
        return null;
    }
    
    public static void prepareNewUser(AccessContext context, User user, String testUserExtension) throws AppException {
        String customer = testCustomerFromName(user.lastname);
        if (customer == null) customer = testUserExtension;
        
        if (customer != null) {
            user.testUserApp = context.getUsedPlugin();
            user.testUserCustomer = customer;
        }
    }
    
    public static void createNewUser(AccessContext context, User user) throws AppException {
        
    }
}
