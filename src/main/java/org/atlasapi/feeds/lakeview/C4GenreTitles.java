package org.atlasapi.feeds.lakeview;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class C4GenreTitles {
    
    private static final Map<String, String> titles = ImmutableMap.<String,String>builder()
        .put("http://www.channel4.com/programmes/categories/animals", "Animals")
        .put("http://www.channel4.com/programmes/categories/animation", "Animation")
        .put("http://www.channel4.com/programmes/categories/art-design-and-literature", "Art, Design and Literature")
        .put("http://www.channel4.com/programmes/categories/audiodescribed", "Audio Described")
        .put("http://www.channel4.com/programmes/categories/business-and-money", "Business and Money")
        .put("http://www.channel4.com/programmes/categories/chat-shows", "Chat Shows")
        .put("http://www.channel4.com/programmes/categories/childrens-shows", "Children's Shows")
        .put("http://www.channel4.com/programmes/categories/comedy", "Comedy")
        .put("http://www.channel4.com/programmes/categories/disability", "Disability")
        .put("http://www.channel4.com/programmes/categories/documentaries", "Documentaries")
        .put("http://www.channel4.com/programmes/categories/drama", "Drama")
        .put("http://www.channel4.com/programmes/categories/e4", "E4")
        .put("http://www.channel4.com/programmes/categories/education-and-learning", "Education and Learning")
        .put("http://www.channel4.com/programmes/categories/entertainment", "Entertainment")
        .put("http://www.channel4.com/programmes/categories/family-and-parenting", "Family and Parenting")
        .put("http://www.channel4.com/programmes/categories/fashion-and-beauty", "Fashion and Beauty")
        .put("http://www.channel4.com/programmes/categories/film", "Film")
        .put("http://www.channel4.com/programmes/categories/food", "Food")
        .put("http://www.channel4.com/programmes/categories/health-and-wellbeing", "Health and Wellbeing")
        .put("http://www.channel4.com/programmes/categories/history", "History")
        .put("http://www.channel4.com/programmes/categories/homes-and-gardens", "Homes and Gardens")
        .put("http://www.channel4.com/programmes/categories/lifestyle", "Lifestyle")
        .put("http://www.channel4.com/programmes/categories/more4", "More4")
        .put("http://www.channel4.com/programmes/categories/music", "Music")
        .put("http://www.channel4.com/programmes/categories/news-current-affairs-and-politics", "News, Current Affairs and Politics")
        .put("http://www.channel4.com/programmes/categories/quizzes-and-gameshows", "Quizzes and Gameshows")
        .put("http://www.channel4.com/programmes/categories/reality-shows", "Reality Shows")
        .put("http://www.channel4.com/programmes/categories/religion-and-belief", "Religion and Belief")
        .put("http://www.channel4.com/programmes/categories/science-nature-and-the-environment", "Science, Nature and the Environment")
        .put("http://www.channel4.com/programmes/categories/sex-and-relationships", "Sex and Relationships")
        .put("http://www.channel4.com/programmes/categories/society-and-culture", "Society and Culture")
        .put("http://www.channel4.com/programmes/categories/sports-and-games", "Sports and Games")
        .put("http://www.channel4.com/programmes/categories/us-shows", "US Shows")
        .put("http://www.channel4.com/programmes/categories/factual", "Factual")
        .put("http://www.channel4.com/programmes/categories/sport", "Sport")
    .build();

    public static String title(String genreUri) {
        return titles.get(genreUri);
    }
}
