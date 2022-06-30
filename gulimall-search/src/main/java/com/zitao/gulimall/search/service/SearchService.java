package com.zitao.gulimall.search.service;


import com.zitao.gulimall.search.vo.SearchParam;
import com.zitao.gulimall.search.vo.SearchResult;

public interface SearchService {
    SearchResult getSearchResult(SearchParam searchParam);
}
