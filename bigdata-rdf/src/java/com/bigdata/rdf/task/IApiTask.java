/*

 Copyright (C) SYSTAP, LLC 2006-2008.  All rights reserved.

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
package com.bigdata.rdf.task;

import java.util.concurrent.Callable;

import com.bigdata.journal.IIndexManager;

/**
 * A task that can be run either with direct index access or using the
 * concurrency manager (compatible with group commit).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see <a href="- http://sourceforge.net/apps/trac/bigdata/ticket/566" >
 *      Concurrent unisolated operations against multiple KBs </a>
 */
public interface IApiTask<T> extends Callable<T> {

   /** The namespace of the target KB instance. */
   String getNamespace();

   /** The timestamp of the view of that KB instance. */
   long getTimestamp();

   /**
    * Tasks that create or destroy locatable resources need to write on the
    * global row store (GRS). Such tasks must specify this property. Note that
    * the GRS lock will have the side-effect of serializing all such tasks that
    * write on the GRS index.
    */
   boolean isGRSRequired();
   
   /**
    * Protocol used to inform the task of the index manager before it is
    * executed.
    * 
    * @throws IllegalStateException
    *            if the index manager has already been set.
    */
   void setIndexManager(IIndexManager indexManager);

   /**
    * Protocol used to clear the index manager from the task after it is
    * executed.
    */
   void clearIndexManager();

}