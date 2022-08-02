package com.zitao.gulimall.search.controller;


import com.zitao.gulimall.search.service.SearchService;
import com.zitao.gulimall.search.vo.SearchParam;
import com.zitao.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 自动将页面传递的查询参数封装为vo对象 SearchParam
     *
     * @param searchParam
     * @param model
     * @param request
     * @return
     */
    @GetMapping(value = {"/list.html", "/"})
    public String getSearchPage(SearchParam searchParam, Model model, HttpServletRequest request) {
        searchParam.set_queryString(request.getQueryString());
        SearchResult result = searchService.getSearchResult(searchParam);
        model.addAttribute("result", result);
        return "list";
    }
}
