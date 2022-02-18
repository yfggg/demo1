package com.example.demo.utils;


import cn.hutool.core.text.StrSplitter;
import cn.hutool.dfa.WordTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SensitiveWordUtil {

    public static List<String> matchAll(String text, String sensitiveWords, boolean isDensityMatch, boolean isGreedMatch) {
        //参数：被切分字符串，分隔符逗号，0表示无限制分片数，去除两边空格，忽略空白项
        List<String> sensitiveWordList = StrSplitter.split(sensitiveWords,',',0,true,true);
        WordTree tree = new WordTree();
        for (String sensitiveWord : sensitiveWordList) {
            tree.addWord(sensitiveWord);
        }
        // 标准匹配，匹配到最短关键词，并跳过已经匹配的关键词
        return tree.matchAll(text, -1, isDensityMatch, isGreedMatch);
    }

}
