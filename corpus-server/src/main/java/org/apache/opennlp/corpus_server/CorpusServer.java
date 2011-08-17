/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.opennlp.corpus_server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.opennlp.corpus_server.search.LuceneSearchService;
import org.apache.opennlp.corpus_server.search.SearchService;
import org.apache.opennlp.corpus_server.store.CorporaChangeListener;
import org.apache.opennlp.corpus_server.store.CorporaStore;
import org.apache.opennlp.corpus_server.store.CorpusStore;
import org.apache.opennlp.corpus_server.store.DerbyCorporaStore;

public class CorpusServer implements ServletContextListener {

  static class IndexListener implements CorporaChangeListener {
    
    private final SearchService searchService;
    
    IndexListener(SearchService searchService) {
      this.searchService = searchService;
    }
    
    @Override
    public void addedCAS(CorpusStore store, String casId) {
      try {
        searchService.index(store, casId);
      } catch (IOException e) {
        // TODO: Also log store name!
        LOGGER.warning("Failed to index cas: " + casId);
      }
    }

    @Override
    public void updatedCAS(CorpusStore store, String casId) {
      addedCAS(store, casId);
    }
  }
  
  private final static Logger LOGGER = Logger.getLogger(
      CorpusServer.class .getName());
  
  private static CorpusServer instance;
  
  private CorporaStore store;
  private SearchService searchService;
  
  private CorporaChangeListener indexListener;
  
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    
    instance = this;
    
    store = new DerbyCorporaStore();
    try {
      store.initialize();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to start corpora store!", e);
    }
    
    searchService = new LuceneSearchService();
    
    try {
      searchService.initialize(store);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to start search service!", e);
    }
    
    indexListener = new IndexListener(searchService);
    store.addCorpusChangeListener(indexListener);
  }
  
  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    
    // Note: 
    // Everything should be shutdown in the opposite
    // order than the startup.
    
    store.removeCorpusChangeListener(indexListener);
    
    if (searchService != null) {
      try {
        searchService.shutdown();
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to shutdown search service!", e);
      }
    }
    
    if (store != null)
      try {
        store.shutdown();
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to shutdown corpora store!", e);
      }
  }

  public CorporaStore getStore() {
    return store;
  }
  
  public SearchService getSearchService() {
    return searchService;
  }
  
  public static CorpusServer getInstance() {
    return instance;
  }
}