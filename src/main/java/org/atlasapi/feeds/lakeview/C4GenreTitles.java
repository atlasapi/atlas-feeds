package org.atlasapi.feeds.lakeview;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class C4GenreTitles {
    
    private static final Map<String, String> titles = ImmutableMap.<String,String>builder()
        .put("http://www.channel4.com/programmes/tags/animals", "Animals")
        .put("http://www.channel4.com/programmes/tags/animation", "Animation")
        .put("http://www.channel4.com/programmes/tags/art-design-and-literature", "Art, Design and Literature")
        .put("http://www.channel4.com/programmes/tags/audiodescribed", "Audio Described")
        .put("http://www.channel4.com/programmes/tags/business-and-money", "Business and Money")
        .put("http://www.channel4.com/programmes/tags/chat-shows", "Chat Shows")
        .put("http://www.channel4.com/programmes/tags/childrens-shows", "Children's Shows")
        .put("http://www.channel4.com/programmes/tags/comedy", "Comedy")
        .put("http://www.channel4.com/programmes/tags/disability", "Disability")
        .put("http://www.channel4.com/programmes/tags/documentaries", "Documentaries")
        .put("http://www.channel4.com/programmes/tags/drama", "Drama")
        .put("http://www.channel4.com/programmes/tags/e4", "E4")
        .put("http://www.channel4.com/programmes/tags/education-and-learning", "Education and Learning")
        .put("http://www.channel4.com/programmes/tags/entertainment", "Entertainment")
        .put("http://www.channel4.com/programmes/tags/family-and-parenting", "Family and Parenting")
        .put("http://www.channel4.com/programmes/tags/fashion-and-beauty", "Fashion and Beauty")
        .put("http://www.channel4.com/programmes/tags/film", "Film")
        .put("http://www.channel4.com/programmes/tags/food", "Food")
        .put("http://www.channel4.com/programmes/tags/health-and-wellbeing", "Health and Wellbeing")
        .put("http://www.channel4.com/programmes/tags/history", "History")
        .put("http://www.channel4.com/programmes/tags/homes-and-gardens", "Homes and Gardens")
        .put("http://www.channel4.com/programmes/tags/lifestyle", "Lifestyle")
        .put("http://www.channel4.com/programmes/tags/more4", "More4")
        .put("http://www.channel4.com/programmes/tags/music", "Music")
        .put("http://www.channel4.com/programmes/tags/news-current-affairs-and-politics", "News, Current Affairs and Politics")
        .put("http://www.channel4.com/programmes/tags/quizzes-and-gameshows", "Quizzes and Gameshows")
        .put("http://www.channel4.com/programmes/tags/reality-shows", "Reality Shows")
        .put("http://www.channel4.com/programmes/tags/religion-and-belief", "Religion and Belief")
        .put("http://www.channel4.com/programmes/tags/science-nature-and-the-environment", "Science, Nature and the Environment")
        .put("http://www.channel4.com/programmes/tags/sex-and-relationships", "Sex and Relationships")
        .put("http://www.channel4.com/programmes/tags/society-and-culture", "Society and Culture")
        .put("http://www.channel4.com/programmes/tags/sports-and-games", "Sports and Games")
        .put("http://www.channel4.com/programmes/tags/us-shows", "US Shows")
        .put("http://www.channel4.com/programmes/tags/factual", "Factual")
        .put("http://www.channel4.com/programmes/tags/sport", "Sport")
    .build();

    public static String title(String genreUri) {
        return titles.get(genreUri);
    }
}
