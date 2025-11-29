package com.dian.demo.di.model

data class ListData<T>(val curPage: String? = null,val offset: String? = null,val over: Boolean? = null,val pageCount: String? = null,val size: String? = null,val total: String? = null,val datas: List<T>? = null
)

/**
 * apkLink :
 * author : 玉刚说
 * chapterId : 410
 * chapterName : 玉刚说
 * collect : false
 * courseId : 13
 * desc :
 * envelopePic :
 * fresh : false
 * id : 8367
 * link : https://mp.weixin.qq.com/s/uI7Fej1_qSJOJnzQ6offpw
 * niceDate : 2019-05-06
 * origin :
 * prefix :
 * projectLink :
 * publishTime : 1557072000000
 * superChapterId : 408
 * superChapterName : 公众号
 * tags : [{"name":"公众号","url":"/wxarticle/list/410/1"}]
 * title : 深扒 EventBus：register
 * type : 0
 * userId : -1
 * visible : 1
 * zan : 0
 */
data class ArticleBean(var apkLink: String? = null, var author: String? = null,
                       var shareUser: String? = null,
                       var chapterId: Int? = 0,
                       var chapterName: String? = null,
                       var collect: Boolean? = false,
                       var courseId: Int?  = 0,
                       var desc: String? = null,
                       var envelopePic: String? = null,
                       var top:Boolean = false,
                       var fresh:Boolean = false,
                       var id: Int  = 0,
                       var link: String? = null,
                       var niceDate: String? =null,
                       var origin: String? =null,
                       var prefix: String? = null,
                       var projectLink: String? = null,
                       var publishTime: Long =0,
                       var superChapterId: Int?  = 0,
                       var superChapterName: String? = null,
                       var title: String? = null,
                       var type: Int?  = 0,
                       var userId: Int?  = 0,
                       var visible: Int?  = 0,
                       var zan: Int?  = 0,
                       var tags: MutableList<TagsBean?>? = null,
                       var originId: Int?  = 0
)

data class TagsBean(var name: String? = null,var url: String? = null
)