package system.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import system.access.BannedChannelDAO;
import system.access.BannedTagDAO;

public class Feed
{
    private static final Logger logger = LogManager.getLogger(Feed.class);

    private List<String> bannedChannels;
    private List<String> bannedTags;

    private List<Video> videos = new ArrayList<>();

    public List<Video> getVideos()
    {
        return videos;
    }

    public void setVideos(List<Video> videos)
    {
        this.videos = videos;
    }

    public List<Video> filtration(String chatId)
    {
        bannedChannels = BannedChannelDAO.getInstance().getBannedChannels(chatId);
        bannedTags = BannedTagDAO.getInstance().getBannedTags(chatId);

        return videos.stream()
                     .filter(this::oldFilter)
                     .filter(this::tagNameFilter)
                     .filter(this::tagDescriptionFilter)
                     .filter(this::channelFilter)
                     .sorted((v2, v1) -> (int)(v1.getViewCount() - v2.getViewCount()))
                     .limit(10)
                     .collect(Collectors.toList());
    }

    private boolean tagNameFilter(Video video)
    {
        for (String word : bannedTags)
        {
            Pattern pattern = Pattern.compile(word.toLowerCase());
            Matcher matcher = pattern.matcher(video.getName().toLowerCase());

            if (matcher.find())
            {
                logger.info("Тег-фильтрация в названии: [{}] {}" , word, video.getName());
                return false;
            }
        }

        return true;
    }

    private boolean tagDescriptionFilter(Video video)
    {
        if (video.getDescription() == null)
        {
            return true;
        }

        for (String word : bannedTags)
        {
            Pattern pattern = Pattern.compile(word.toLowerCase());
            Matcher matcher = pattern.matcher(video.getDescription().toLowerCase());

            if (matcher.find())
            {
                logger.info("Тег-фильтрация в описании: [{}]{} ", word, video.getDescription());
                return false;
            }
        }

        return true;
    }

    private boolean oldFilter(Video video)
    {
        try
        {
            if (video.getOld() == null)
            {
                logger.warn("Video no have old. {}", video.toString());
                return false;
            }

            // Если не hours, тогда будет day(s), week но нам это ненадо
            // У нас гораничение 24 часа
            return video.getOld().matches("[0-9]+ hour.*");
        }
        catch (Exception e)
        {
            logger.error("Error on filtration by old", e);
        }

        return false;
    }

    private boolean channelFilter(Video video)
    {
        for (String channel : bannedChannels)
        {
            Pattern pattern = Pattern.compile(channel.toLowerCase());
            Matcher matcher = pattern.matcher(video.getChannel().toLowerCase());

            if (matcher.find())
            {
                logger.info("Канал-фильтрация: [{}] {}", channel, video.getName());
                return false;
            }
        }

        return true;
    }
}
