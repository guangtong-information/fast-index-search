package com.fis.web.modules.songmain.cache;

import com.fis.web.constant.SortDirection;
import com.fis.web.init.threadwork.AppAction;
import com.fis.web.modules.songmain.db.impl.MyBatisDaoImpl;
import com.fis.web.modules.songmain.model.SongMain;
import com.fis.web.modules.songmain.model.SongMainGroup;
import com.fis.web.redis.base.RedisUtil;
import com.fis.web.tools.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class SongMainCache {

    private static int cacheType = 1;  //1.本地缓存 2.redis缓存
    private static String key = "fis-index-web:SongMain";
    private static String indexKey = "fis-index-web:SongMainIndexKey";
    private static List<SongMain> SongMainList = new ArrayList<SongMain>();

    private static HashMap<String, Integer[]> _listIndex = new HashMap<String, Integer[]>();
    private static HashMap<String, Integer> _listRows = new HashMap<String, Integer>();

    // 锁对象
    private final static ReentrantLock lock = new ReentrantLock();

    @Autowired
    private MyBatisDaoImpl myBatisDaoImpl;

    @Autowired
    private RedisUtil redisUtil;

    // 用于全量缓存
    @Autowired(required = false)
    private RedisTemplate redisTemplate;

    public ListOperations<String, SongMain> getListOperations() {
        return redisTemplate.opsForList();
    }


    // / <summary>
    // / 队列操作静态缓存
    // / </summary>
    // / <param name="action"></param>
    // / <param name="value"></param>
    public void AsyncDeal(AppAction action, String value) {
        try {
            if (action == AppAction.Load) {
                LoadAllListCache();
                log.info("========起步加载SongMain整表完成========");
            } else if (action == AppAction.All) {
                UpdateAllListCache();
                log.info("========后台定点或监控更新SongMain整表缓存完成========");
            } else if (action == AppAction.Check) {
                LoadAllListCache();
                log.info("========检查SongMain加载情况========");
            } else if (action == AppAction.Remove) {
            }
        } catch (Exception ex) {
            log.error("SongMain.AsyncDeal()" + ex);
        }
    }

    public List<SongMain> GetAllListCache() {
        // 判断整表是否有数据
        if (SongMainList != null && SongMainList.size() > 0)
            return SongMainList;

        return new ArrayList<SongMain>();
    }

    /**
     * 删除缓存
     */
    public void killCache() {
        if (cacheType == 1) {
            //清空本地缓存
            SongMainList.clear();
        } else {
            //清空redis缓存
            redisUtil.delete(key);
        }

        //本地索引缓存
        _listRows.clear();
        _listIndex.clear();
        //gc回收
        System.gc();
    }

    // / <summary>
    // / 普通列表调用（入口）
    // / </summary>
    // / <returns></returns>
    public SongMainGroup GetAllCache(int pageNow, int pageSize,
                                     String SongName, String sortDirection, String sortExpression) {

        //=========== 01 验证数据 ==============
        Boolean isExists = false;
        //如果有数据在加载，则需要等待数据加载完毕，数据不完整不能读取制作索引
        if (isUserDoing) {
            SongMainGroup smg = new SongMainGroup();
            smg.SongMainList = new ArrayList<SongMain>();
            return smg;
        }

        if (cacheType == 1) {
            //本地静态变量
            if (SongMainList != null && SongMainList.size() > 0) {
                isExists = true;
            }
        } else {
            //redis缓存
            isExists = redisTemplate.hasKey(key);
        }

        // 整表没有数据
        if (!isExists) {
            SongMainGroup smg = new SongMainGroup();
            smg.SongMainList = new ArrayList<SongMain>();
            return smg;
        }

        //=========== 02 索引制造 ==============
        Integer rows = 0;
        List<SongMain> SongMainList_Cache = null;
        Integer[] index = null;
        String searchKey = "indexKey_SongMainList_index_" + pageSize + "_"
                + SongName + "_" + sortDirection + "_" + sortExpression;

        if (cacheType == 1) {
            //获取本地索引缓存
            index = _listIndex.get(searchKey);

            //制造索引
            if (index != null && index.length > 0) {
                rows = index.length;
            } else {
                //本地静态变量
                SongMainList_Cache = new ArrayList<SongMain>();
                SongMainList_Cache.addAll(SongMainList);

                //算出缓存索引位置
                index = GetListIndex(SongMainList_Cache, SongName, sortDirection, sortExpression);

                //索引缓存
                rows = index.length;
                if (rows > 0) {
                    _listIndex.put(indexKey, index);
                }
            }
        } else {
            //获取redis索引缓存
            HashOperations<String, String, Integer[]> vo = redisTemplate.opsForHash();
            index = vo.get(indexKey, searchKey);

            //制造索引
            if (index != null && index.length > 0) {
                rows = index.length;
            } else {
                SongMainList_Cache = getListOperations().range(key, 0, 99999);

                //算出缓存索引位置
                index = GetListIndex(SongMainList_Cache, SongName, sortDirection, sortExpression);

                //索引缓存
                rows = index.length;
                if (rows > 0) {
                    vo.put(indexKey, searchKey, index);
                }
            }
        }

        //释放中间变量
        SongMainList_Cache = null;

        //=========== 03 索引抽数 ==============
        // 逻辑计算当页索引范围
        int endNum = 0, startNum = 0, realcount = 0;
        startNum = (pageNow - 1) * pageSize;
        endNum = (pageNow * pageSize) - 1;
        if (endNum >= rows)
            endNum = rows;
        realcount = endNum - startNum + 1;

        //有数据
        if (endNum >= startNum && realcount > 0) {
            // 加载list
            List<SongMain> list = new ArrayList<SongMain>(realcount);
            for (int i = startNum; i <= endNum && i < rows; i++) {
                if (cacheType == 1) {
                    //静态变量
                    list.add(SongMainList.get(index[i]));
                } else {
                    list.add(getListOperations().index(key, index[i]));
                }
            }

            // 释放
            index = null;
            //gc回收
            System.gc();

            SongMainGroup GL = new SongMainGroup();
            GL.SongMainList = list;
            GL.TotalRows = rows;
            Double totalPage = Math.ceil((double) rows / pageSize);
            GL.TotalPage = totalPage.intValue();
            return GL;
        }

        // 释放
        index = null;
        //gc回收
        System.gc();

        SongMainGroup smg = new SongMainGroup();
        smg.SongMainList = new ArrayList<SongMain>();
        return smg;
    }

    // / <summary>
    // / 获取列表索引
    // / </summary>
    private Integer[] GetListIndex(List<SongMain> songMainList,
                                   String SongName, String sortDirection, String sortExpression) {
        Integer[] index = null;
        Integer rows = 0;
        if (songMainList == null || songMainList.size() <= 0) {
            return new Integer[0];
        }
        try {
            List<SongMain> temp = new ArrayList<>();
            // 关键字搜索
            if (StringUtils.hasText(SongName)) {

                // 完全匹配
                List<SongMain> temp1 = getWholeMatch(songMainList, SongName);
                // 模糊匹配
                List<SongMain> temp2 = getFuzzyMatch(songMainList, SongName);
                temp2 = GetSort(temp2, sortDirection, sortExpression);
                temp.addAll(temp1);
                temp.addAll(temp2);
                temp1 = null;
                temp2 = null;
            } else {
                // 排序
                temp = GetSort(songMainList, sortDirection, sortExpression);
            }

            // 获取数据条数
            rows = temp.size();

            // 加载list列表
            if (rows > 0) {
                int w = 0;
                index = new Integer[rows];
                // 循环添加索引
                for (SongMain songmod : temp) {
                    index[w] = songmod.IdRank;
                    w++;
                }
            } else {
                return new Integer[0];
            }

        } catch (Exception ex) {
            log.error("GetListIndex():" + ex);
            return new Integer[0];
        }
        return index;
    }

    // / <summary>
    // / 排序
    // / </summary>
    protected List<SongMain> GetSort(List<SongMain> SortList,
                                     final String sortDirection, String sortExpression) {
        sortExpression = sortExpression.toLowerCase();
        //排序规则:优先sortnum > stime

        if (sortExpression.contains("stime")) {
            // 发布时间
            Collections.sort(SortList, new Comparator<SongMain>() {
                @Override
                public int compare(SongMain object1, SongMain object2) {
                    try {
                        Date time1 = object1.Stime;
                        Date time2 = object2.Stime;

                        if (SortDirection.Desc.getCode().equals(sortDirection)) {
                            return time2.compareTo(time1);
                        } else {
                            return time1.compareTo(time2);
                        }
                    } catch (Exception e) {
                        log.error("对比时间错误", e);
                    }

                    return 0;
                }
            });
        }

        if (sortExpression.contains("sortnum")) {
            // 自定义顺序
            Collections.sort(SortList, new Comparator<SongMain>() {
                @Override
                public int compare(SongMain object1, SongMain object2) {
                    if (SortDirection.Desc.getCode().equals(sortDirection))
                        return object2.SortNum.compareTo(object1.SortNum);
                    else
                        return object1.SortNum.compareTo(object2.SortNum);
                }
            });
        }

        return SortList;
    }

    // / <summary>
    // / 名称搜索(完全匹配)
    // / </summary>
    protected List<SongMain> getWholeMatch(List<SongMain> SearchList, String kw) {
        try {
            List<SongMain> myList = new ArrayList<SongMain>();
            kw = kw.trim().toLowerCase();

            // 完全匹配
            for (SongMain s : SearchList) {
                if (s.SongName.equals(kw)
                        || ("," + s.KeyWord + ",").equals(("," + kw + ",")))
                    myList.add(s);
            }

            return myList;
        } catch (Exception ex) {
            log.error("", ex);
            return SearchList;
        }
    }

    // / <summary>
    // / 名称搜索(模糊匹配)
    // / </summary>
    protected List<SongMain> getFuzzyMatch(List<SongMain> SearchList, String kw) {
        try {
            List<SongMain> myList = new ArrayList<SongMain>();
            kw = kw.trim().toLowerCase();

            // 模糊匹配
            for (SongMain s : SearchList) {
                if ((s.SongName.contains(kw) || s.KeyWord.contains(kw))
                        && !s.SongName.equals(kw) && !s.KeyWord.equals(kw))
                    myList.add(s);
            }

            return myList;
        } catch (Exception ex) {
            log.error("", ex);
            return SearchList;
        }
    }

    static boolean isUserDoing = false;

    // / <summary>
    // / 存在不更新缓存
    // / </summary>
    // / <returns></returns>
    private void LoadAllListCache() {
        boolean isFirst = false;
        boolean isExists = false;

        if (cacheType == 1) {
            //本地静态变量
            if (SongMainList != null && SongMainList.size() > 0) {
                isExists = true;
            }
        } else {
            //redis缓存
            isExists = redisTemplate.hasKey(key);
        }

        // 判断是否首次加载
        if (!isExists) {
            isFirst = true;
        }

        // 用户如果在操作数据，请等待
        while (isUserDoing && isFirst) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.error("", e);
            }
        }

        // 如果没有加载缓存，则加载
        if (isFirst) {
            isUserDoing = true;
            try {
                lock.lock();

                //本地索引缓存
                _listRows.clear();
                _listIndex.clear();

                //读取数据库
                List<SongMain> list = myBatisDaoImpl.getSongMainList();

                if (cacheType == 1) {
                    //清空本地缓存
                    SongMainList.clear();
                    //加载本地缓存
                    SongMainList.addAll(list);
                } else {
                    //清空redis缓存
                    redisTemplate.delete(key);
                    //设置全量缓存
                    getListOperations().rightPushAll(key, list);
                }

                log.info("加载SongMain整表完成!");
            } catch (Exception ex) {
                log.error("加载SongMain整表:", ex);
            } finally {
                isUserDoing = false;
                //回收内存
                System.gc();
                lock.unlock();
            }
        }

    }

    // / <summary>
    // / 强制更新缓存
    // / </summary>
    // / <returns></returns>
    private void UpdateAllListCache() {
        boolean isFirst = true;

        // 用户如果在操作数据，请等待
        while (isUserDoing && isFirst) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.error("加载SongMain整表:", e);
            }
        }

        if (isFirst) {
            isUserDoing = true;
            try {
                lock.lock();

                //本地索引缓存
                _listRows.clear();
                _listIndex.clear();

                //读取数据库
                List<SongMain> list = myBatisDaoImpl.getSongMainList();

                if (cacheType == 1) {
                    //清空本地缓存
                    SongMainList.clear();
                    //加载本地缓存
                    SongMainList.addAll(list);
                } else {
                    //清空redis缓存
                    redisTemplate.delete(key);
                    //设置全量缓存
                    getListOperations().rightPushAll(key, list);
                }

                log.info("强制加载SongMain整表完成!");

            } catch (Exception ex) {
                log.error("强制加载SongMain整表:", ex);
            } finally {
                isUserDoing = false;
                //回收内存
                System.gc();
                lock.unlock();
            }
        }

    }
}
