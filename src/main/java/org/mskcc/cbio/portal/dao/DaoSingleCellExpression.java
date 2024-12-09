/*
 * Copyright (c) 2024 The Hyve B.V.
 * This code is licensed under the GNU Affero General Public License (AGPL),
 * version 3, or (at your option) any later version.
 */

/*
 * This file is part of cBioPortal.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 * @author Matthijs Pon
*/

package org.mskcc.cbio.portal.dao;

import java.sql.*;
import org.mskcc.cbio.portal.model.*;

public class DaoSingleCellExpression {
	
	private DaoSingleCellExpression() {
	}
	
    public static void addSingleCellExpression(SingleCellExpression singleCellExpression) throws DaoException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        
        try {
        	// Open connection to database
            connection = JdbcUtil.getDbConnection(DaoGeneticProfileLink.class);
	        
	        // Prepare SQL statement
            preparedStatement = connection.prepareStatement("INSERT INTO single_cell_expression "
                + "(GENETIC_PROFILE_ID, SAMPLE_ID, TISSUE, CELL_TYPE, ENTREZ_GENE_ID, EXPRESSION_VALUE) VALUES (?,?,?,?,?,?)");
            // Fill in statement
            preparedStatement.setInt(1, singleCellExpression.getGeneticProfileId());
            preparedStatement.setInt(2, singleCellExpression.getSampleId());
            preparedStatement.setString(3, singleCellExpression.getTissue());
            preparedStatement.setString(4, singleCellExpression.getCellType());
            preparedStatement.setLong(5, singleCellExpression.getGene().getEntrezGeneId());
            preparedStatement.setString(6, singleCellExpression.getExpressionValue());
            
            // Execute statement
            preparedStatement.execute();
        } catch (SQLException e) {
            throw new DaoException(e);
        } finally {
            JdbcUtil.closeAll(DaoGeneticProfileLink.class, connection, preparedStatement, resultSet);
        }
    }
}
