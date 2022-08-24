package com.parchment.ipp.proxy.gateway.predicates;

import com.parchment.ipp.proxy.gateway.services.IppMemberPrinterService;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

public class PrinterRoutePredicateFactory extends AbstractRoutePredicateFactory<PrinterRoutePredicateFactory.Config> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IppMemberPrinterService ippMemberPrinterService;

    public PrinterRoutePredicateFactory(IppMemberPrinterService ippMemberPrinterService) {
        super(Config.class);
        this.ippMemberPrinterService = ippMemberPrinterService;
    }

    @Override
    public Predicate<ServerWebExchange> apply(PrinterRoutePredicateFactory.Config config) {
        return (ServerWebExchange swe) -> {
            String printerName = swe.getRequest().getPath()
                    .subPath(swe.getRequest().getPath().elements().size() - 1).value();
              boolean isPappl = ippMemberPrinterService.checkPrinterEnvironment(printerName);
              logger.info("Is printer production {}", isPappl);
              return config.isPappl() ? isPappl: !isPappl;
        };
    }

    @Validated
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public static class Config {

        boolean isPappl = true;

    }
}
