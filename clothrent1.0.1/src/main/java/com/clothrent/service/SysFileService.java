package com.clothrent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clothrent.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 附件表，存放文件 服务类
 * </p>
 *
 * @author liuqiming
 * @since 2021-12-11
 */
public interface SysFileService extends IService<SysFile> {
    SysFile saveOrUpdateFile(SysFile sysFile, MultipartFile file);

    SysFile saveOnly(SysFile sysFile, MultipartFile file);


}
