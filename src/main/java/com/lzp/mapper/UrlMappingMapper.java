package com.lzp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzp.entity.UrlMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 短链接映射 Mapper
 */
@Mapper
public interface UrlMappingMapper extends BaseMapper<UrlMapping> {

    /**
     * 根据短码查询映射记录
     */
    @Select("SELECT * FROM url_mapping WHERE short_code = #{shortCode}")
    UrlMapping selectByShortCode(@Param("shortCode") String shortCode);

    /**
     * 更新访问计数和最近访问时间
     */
    @Update("UPDATE url_mapping SET visit_count = visit_count + 1, last_visit_at = NOW() WHERE id = #{id}")
    int incrementVisitCount(@Param("id") Long id);

    /**
     * 根据原始URL查询（用于判断是否已生成过短链）
     */
    @Select("SELECT * FROM url_mapping WHERE original_url = #{originalUrl} LIMIT 1")
    UrlMapping selectByOriginalUrl(@Param("originalUrl") String originalUrl);
}
