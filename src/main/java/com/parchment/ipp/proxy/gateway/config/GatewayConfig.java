package com.parchment.ipp.proxy.gateway.config;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

import com.parchment.ipp.proxy.gateway.predicates.PrinterRoutePredicateFactory;
import com.parchment.ipp.proxy.gateway.predicates.PrinterRoutePredicateFactory.Config;
import com.parchment.ipp.proxy.gateway.services.IppMemberPrinterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    public RouteLocator ippCustomRouteLocator(RouteLocatorBuilder builder, PrinterRoutePredicateFactory prf) {
        return builder.routes()
                .route("ipp_staging_route", r -> r.path("/printers/*").and()
                        .predicate(prf.apply(
                                new Config(
                                        false)))
                        .uri("https://ipp.staging.escrip-safe.com"))
                .route("ipp_prod_route", r -> r.path("/ipp/print/*").and()
                        .predicate(prf.apply(
                                new Config(
                                        true)))/*.filters(f -> f.filter((exchange, chain) -> {
                            String printerName = exchange.getRequest().getPath()
                                    .subPath(exchange.getRequest().getPath().elements().size() - 1).value();
                            ServerHttpRequest request = exchange.getRequest().mutate()
                                    .path("/ipp/print/" + printerName).build();
                            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, request.getURI());
                            logger.info("Printing request {} ", request.getURI());
                            return chain.filter(exchange.mutate().request(request).build());
                        }))*/
                        .uri("http://kubernetes.docker.internal"))
                .build();
    }

/*    @Bean
    public RouteLocator ippCustomRouteLocator(RouteLocatorBuilder builder, PrinterRoutePredicateFactory prf,
            LoggingGatewayFilterFactory lgf) {
        return builder.routes()
                .route("ipp_staging_route",
                        r -> r.path("/printers/*")
                                .filters(f -> f.filter(lgf.apply(new Config("CustomMessage", true, true))))
                                .uri("https://ipp.staging.escrip-safe.com"))
                .build();

    }*/

  /*  @Bean
    public RouteLocator ippCustomRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("ipp_staging_route",
                        r -> r.path("/printers/*")
                                .uri("https://ipp.staging.escrip-safe.com"))
                .build();

    }*/

    @Bean
    public PrinterRoutePredicateFactory isProd(
            IppMemberPrinterService ippMemberPrinterService) {
        return new PrinterRoutePredicateFactory(ippMemberPrinterService);
    }

}
