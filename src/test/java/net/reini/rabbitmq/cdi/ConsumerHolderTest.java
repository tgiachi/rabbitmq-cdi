package net.reini.rabbitmq.cdi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;

@ExtendWith(MockitoExtension.class)
class ConsumerHolderTest {
  private static final int PREFETCH_COUNT = 5;
  @Mock
  private EventConsumer<TestEvent> eventConsumerMock;
  @Mock
  private ConsumerChannelFactory consumerChannelFactoryMock;
  @Mock
  private Channel channelMock;
  @Mock
  private ConsumerFactory consumerFactoryMock;
  @Mock
  private Consumer consmerMock;
  @Mock
  private List<QueueDeclaration> declarationsListMock;
  @Mock
  private DeclarerRepository<QueueDeclaration> declarerRepositoryMock;

  private ConsumerHolder sut;


  @Test
  void activateAndDeactivate() throws IOException, TimeoutException {
    this.sut = new ConsumerHolder(eventConsumerMock, "queue", false, PREFETCH_COUNT, consumerChannelFactoryMock,
        consumerFactoryMock, declarationsListMock,declarerRepositoryMock);
    Assertions.assertEquals("queue", sut.getQueueName());
    Assertions.assertFalse(sut.isAutoAck());
    when(consumerChannelFactoryMock.createChannel()).thenReturn(channelMock);
    when(consumerFactoryMock.createAcknowledged(eventConsumerMock, channelMock))
        .thenReturn(consmerMock);
    sut.activate();
    verify(channelMock).addShutdownListener(any());
    verify(declarerRepositoryMock).declare(channelMock, declarationsListMock);
    verify(channelMock, never()).close();
    verify(channelMock, never()).removeShutdownListener(any());
    verify(channelMock).basicConsume("queue", false, consmerMock);
    sut.deactivate();
    verify(channelMock).close();
    verify(channelMock).removeShutdownListener(any());
  }

  @Test
  void activateAndDeactivateWithAutoAck() throws IOException, TimeoutException {
    this.sut = new ConsumerHolder(eventConsumerMock, "queue", true, PREFETCH_COUNT, consumerChannelFactoryMock,
        consumerFactoryMock,declarationsListMock,declarerRepositoryMock);
    Assertions.assertEquals("queue", sut.getQueueName());
    Assertions.assertTrue(sut.isAutoAck());
    when(consumerChannelFactoryMock.createChannel()).thenReturn(channelMock);
    when(consumerFactoryMock.create(eventConsumerMock)).thenReturn(consmerMock);
    sut.activate();
    verify(channelMock).addShutdownListener(any());
    verify(channelMock).basicConsume("queue", true, consmerMock);
    verify(declarerRepositoryMock).declare(channelMock,declarationsListMock);
    verify(channelMock, never()).close();
    verify(channelMock, never()).removeShutdownListener(any());
    verify(channelMock).basicQos(PREFETCH_COUNT);

    sut.deactivate();
    verify(channelMock).close();
    verify(channelMock).removeShutdownListener(any());
  }

  @Test
  void errorDuringActivate() {
    Assertions.assertThrows(IOException.class, () -> {
      this.sut = new ConsumerHolder(eventConsumerMock, "queue", true, 0, consumerChannelFactoryMock,
          consumerFactoryMock,declarationsListMock,declarerRepositoryMock);
      when(consumerChannelFactoryMock.createChannel()).thenReturn(channelMock);
      when(consumerFactoryMock.create(eventConsumerMock)).thenReturn(consmerMock);
      doThrow(new IOException()).when(channelMock).basicConsume("queue", true, consmerMock);
      sut.activate();
      verify(channelMock).addShutdownListener(any());
      verify(declarerRepositoryMock).declare(channelMock,declarationsListMock);
      verify(channelMock).close();
      verify(channelMock).removeShutdownListener(any());
    });
  }

}
