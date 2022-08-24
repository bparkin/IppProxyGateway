package com.parchment.ipp.proxy.gateway.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IppPrinterMemberServiceImpl implements IppMemberPrinterService{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean checkPrinterEnvironment(String printerName) {
        logger.info("Checking printer environment {}", printerName);
        if(printerName.equalsIgnoreCase("generic")) {
            return true;
        }
        return false;
    }
}
