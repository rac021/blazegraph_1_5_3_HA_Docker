/**

Copyright (C) SYSTAP, LLC 2006-2011.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Aug 28, 2011
 */

package com.bigdata.rdf.sail.sparql;

import java.util.Properties;

import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParser;

import com.bigdata.rdf.sail.IBigdataParsedQuery;
import com.bigdata.rdf.sail.QueryType;
import com.bigdata.rdf.sparql.ast.QueryRoot;

/**
 * Class extends {@link ParsedQuery} for API compliance with {@link QueryParser}
 * but DOES NOT support ANY aspect of the {@link QueryParser} API. All data
 * pertaining to the parsed query is reported by {@link #getQueryRoot()}. There
 * is NO {@link TupleExpr} associated with the {@link BigdataParsedQuery}.
 * Bigdata uses an entirely different model to represent the parsed query,
 * different optimizers to rewrite the parsed query, and different operations to
 * evaluate the {@link ParsedQuery}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class BigdataParsedQuery extends ParsedQuery implements
        IBigdataParsedQuery {

    final private QueryRoot queryRoot;

    /**
     * 
     */
    public BigdataParsedQuery(final QueryRoot queryRoot) {

        this.queryRoot = queryRoot;

    }

    /**
     * Unsupported operation.
     */
    public BigdataParsedQuery(TupleExpr tupleExpr) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    public BigdataParsedQuery(TupleExpr tupleExpr, Dataset dataset) {
        throw new UnsupportedOperationException();
    }

    public QueryRoot getQueryRoot() {
        return queryRoot;
    }

    @Override
    public QueryType getQueryType() {
        return queryRoot.getQueryType();
    }

    @Override
    public Properties getQueryHints() {
        return queryRoot.getQueryHints();
    }

}