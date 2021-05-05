package segeraroot.quotesource;

import segeraroot.connectivity.impl.Client;
import segeraroot.connectivity.impl.SerializationConnectionCallbackFactory;
import segeraroot.performancecounter.impl.PCHostImpl;
import segeraroot.quotemodel.ReadersVisitor;
import segeraroot.quotemodel.impl.BuilderFactoryImpl;
import segeraroot.quotemodel.impl.MessageDeserializerImpl;

public class ClientMain {
    public static void main(String[] args) {
        var pcHost = new PCHostImpl();
        var quoteDispatcher = new QuoteConsumer<BuilderFactoryImpl>(pcHost, "ABC", "DEF");
        var serializer = new SerializationConnectionCallbackFactory<BuilderFactoryImpl, ReadersVisitor<BuilderFactoryImpl>>(
                MessageDeserializerImpl::new,
                BuilderFactoryImpl::new
        );

        var server = new Client<>("localhost", 9000, serializer.handleMessage(quoteDispatcher));
        server.start();

    }

}
