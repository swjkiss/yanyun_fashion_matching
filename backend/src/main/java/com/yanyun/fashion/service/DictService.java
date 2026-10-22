package com.yanyun.fashion.service;

import com.yanyun.fashion.entity.SysDict;
import com.yanyun.fashion.entity.SysDictItem;
import com.yanyun.fashion.mapper.SysDictItemMapper;
import com.yanyun.fashion.mapper.SysDictMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典服务：字典项缓存 + 值/文本/颜色互转。
 *
 * <p>字典是低频变更的只读数据，进程内缓存即可；后台修改字典后调用 {@link #clear()} 失效。</p>
 *
 * @author yanyun backend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    /** 部位。 */
    public static final String CATEGORY = "fashion_category";
    /** 稀有度。 */
    public static final String RARITY = "fashion_rarity";
    /** 色系。 */
    public static final String COLOR = "fashion_color";
    /** 风格。 */
    public static final String STYLE = "fashion_style";

    private final SysDictMapper dictMapper;

    private final SysDictItemMapper itemMapper;

    private final Map<String, List<SysDictItem>> cache = new ConcurrentHashMap<String, List<SysDictItem>>();

    /**
     * 取某字典的全部启用项（带缓存）。
     *
     * @param dictCode 字典编码
     * @return 字典项，未找到返回空列表
     */
    public List<SysDictItem> items(String dictCode) {
        if (dictCode == null) {
            return Collections.emptyList();
        }
        List<SysDictItem> cached = cache.get(dictCode);
        if (cached != null) {
            return cached;
        }
        List<SysDictItem> loaded = Collections.emptyList();
        try {
            SysDict dict = dictMapper.selectByCode(dictCode);
            if (dict != null) {
                loaded = itemMapper.selectByDictId(dict.getId());
            }
        } catch (Exception e) {
            log.warn("[字典] 加载失败 {}：{}", dictCode, e.getMessage());
        }
        cache.put(dictCode, loaded);
        return loaded;
    }

    /**
     * 值 → 文本。
     *
     * @param dictCode 字典编码
     * @param value    字典值
     * @return 文本，未命中返回原值
     */
    public String text(String dictCode, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        for (SysDictItem item : items(dictCode)) {
            if (value.equals(item.getItemValue())) {
                return item.getItemText();
            }
        }
        return value;
    }

    /**
     * 值 → 颜色。
     *
     * @param dictCode 字典编码
     * @param value    字典值
     * @return 颜色，未命中返回 null
     */
    public String color(String dictCode, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (SysDictItem item : items(dictCode)) {
            if (value.equals(item.getItemValue())) {
                return item.getItemColor();
            }
        }
        return null;
    }

    /**
     * JeecgBoot 形态：{@code GET /sys/dict/getDictItems/{dictCode}} 的 result。
     *
     * @param dictCode 字典编码
     * @return [{text, value, title, color}]
     */
    public List<Map<String, Object>> toDictItems(String dictCode) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SysDictItem item : items(dictCode)) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("text", item.getItemText());
            row.put("value", item.getItemValue());
            row.put("title", item.getItemText());
            if (item.getItemColor() != null) {
                row.put("color", item.getItemColor());
            }
            list.add(row);
        }
        return list;
    }

    /**
     * 选项形态：{value, text, color}。
     *
     * @param dictCode 字典编码
     * @return 选项列表
     */
    public List<Map<String, Object>> toOptions(String dictCode) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SysDictItem item : items(dictCode)) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("value", item.getItemValue());
            row.put("text", item.getItemText());
            if (item.getItemColor() != null) {
                row.put("color", item.getItemColor());
            }
            list.add(row);
        }
        return list;
    }

    /**
     * 字典概览列表（后台数据字典页用）。
     *
     * @return [{id, dictCode, dictName, itemCount}]
     */
    public List<Map<String, Object>> listDicts() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String code : new String[]{CATEGORY, RARITY, COLOR, STYLE}) {
            SysDict dict = dictMapper.selectByCode(code);
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", dict == null ? null : dict.getId());
            row.put("dictCode", code);
            row.put("dictName", dict == null ? code : dict.getDictName());
            row.put("itemCount", items(code).size());
            list.add(row);
        }
        return list;
    }

    /** 清空缓存（字典变更后调用）。 */
    public void clear() {
        cache.clear();
    }
}
