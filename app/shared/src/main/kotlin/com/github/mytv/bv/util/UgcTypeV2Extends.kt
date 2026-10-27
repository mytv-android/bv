package com.github.mytv.bv.util

import android.content.Context
import dev.aaa1115910.biliapi.entity.ugc.UgcTypeV2
import com.github.mytv.bv.R

fun UgcTypeV2.getDisplayName(context: Context) = when (this) {
    UgcTypeV2.Douga -> R.string.ugc_type_v2_douga
    UgcTypeV2.DougaFanAnime -> R.string.ugc_type_v2_douga_fan_anime
    UgcTypeV2.DougaGarageKit -> R.string.ugc_type_v2_douga_garage_kit
    UgcTypeV2.DougaCosplay -> R.string.ugc_type_v2_douga_osplay
    UgcTypeV2.DougaOffline -> R.string.ugc_type_v2_douga_offline
    UgcTypeV2.DougaEditing -> R.string.ugc_type_v2_douga_editing
    UgcTypeV2.DougaCommentary -> R.string.ugc_type_v2_douga_commentary
    UgcTypeV2.DougaQuickView -> R.string.ugc_type_v2_douga_quick_view
    UgcTypeV2.DougaVoice -> R.string.ugc_type_v2_douga_voice
    UgcTypeV2.DougaInformation -> R.string.ugc_type_v2_douga_information
    UgcTypeV2.DougaInterpret -> R.string.ugc_type_v2_douga_interpret
    UgcTypeV2.DougaVup -> R.string.ugc_type_v2_douga_vup
    UgcTypeV2.DougaTokusatsu -> R.string.ugc_type_v2_douga_tokusatsu
    UgcTypeV2.DougaPuppetry -> R.string.ugc_type_v2_douga_puppetry
    UgcTypeV2.DougaComic -> R.string.ugc_type_v2_douga_comic
    UgcTypeV2.DougaMotion -> R.string.ugc_type_v2_douga_motion
    UgcTypeV2.DougaReaction -> R.string.ugc_type_v2_douga_reaction
    UgcTypeV2.DougaTutorial -> R.string.ugc_type_v2_douga_tutorial
    UgcTypeV2.DougaOther -> R.string.ugc_type_v2_douga_other

    UgcTypeV2.Game -> R.string.ugc_type_v2_game
    UgcTypeV2.GameRpg -> R.string.ugc_type_v2_game_rpg
    UgcTypeV2.GameMmorpg -> R.string.ugc_type_v2_game_mmorpg
    UgcTypeV2.GameStandAlone -> R.string.ugc_type_v2_game_stand_alone
    UgcTypeV2.GameSlg -> R.string.ugc_type_v2_game_slg
    UgcTypeV2.GameTbs -> R.string.ugc_type_v2_game_tbs
    UgcTypeV2.GameRts -> R.string.ugc_type_v2_game_rts
    UgcTypeV2.GameMoba -> R.string.ugc_type_v2_game_moba
    UgcTypeV2.GameStg -> R.string.ugc_type_v2_game_stg
    UgcTypeV2.GameSpg -> R.string.ugc_type_v2_game_spg
    UgcTypeV2.GameAct -> R.string.ugc_type_v2_game_act
    UgcTypeV2.GameMsc -> R.string.ugc_type_v2_game_msc
    UgcTypeV2.GameSim -> R.string.ugc_type_v2_game_sim
    UgcTypeV2.GameOtome -> R.string.ugc_type_v2_game_otome
    UgcTypeV2.GamePuz -> R.string.ugc_type_v2_game_puz
    UgcTypeV2.GameSandbox -> R.string.ugc_type_v2_game_sandbox
    UgcTypeV2.GameOther -> R.string.ugc_type_v2_game_other

    UgcTypeV2.Kichiku -> R.string.ugc_type_v2_kichiku
    UgcTypeV2.KichikuGuide -> R.string.ugc_type_v2_kichiku_guide
    UgcTypeV2.KichikuTheatre -> R.string.ugc_type_v2_kichiku_theatre
    UgcTypeV2.KichikuManualVocaloid -> R.string.ugc_type_v2_kichiku_manual_vocaloid
    UgcTypeV2.KichikuMad -> R.string.ugc_type_v2_kichiku_mad
    UgcTypeV2.KichikuOther -> R.string.ugc_type_v2_kichiku_other

    UgcTypeV2.Music -> R.string.ugc_type_v2_music
    UgcTypeV2.MusicOriginal -> R.string.ugc_type_v2_music_original
    UgcTypeV2.MusicMv -> R.string.ugc_type_v2_music_mv
    UgcTypeV2.MusicLive -> R.string.ugc_type_v2_music_live
    UgcTypeV2.MusicFanVideos -> R.string.ugc_type_v2_music_fan_videos
    UgcTypeV2.MusicCover -> R.string.ugc_type_v2_music_cover
    UgcTypeV2.MusicPerform -> R.string.ugc_type_v2_music_perform
    UgcTypeV2.MusicVocaloid -> R.string.ugc_type_v2_music_vocaloid
    UgcTypeV2.MusicAiMusic -> R.string.ugc_type_v2_music_ai_music
    UgcTypeV2.MusicRadio -> R.string.ugc_type_v2_music_radio
    UgcTypeV2.MusicTutorial -> R.string.ugc_type_v2_music_tutorial
    UgcTypeV2.MusicCommentary -> R.string.ugc_type_v2_music_commentary
    UgcTypeV2.MusicOther -> R.string.ugc_type_v2_music_other

    UgcTypeV2.Dance -> R.string.ugc_type_v2_dance
    UgcTypeV2.DanceOtaku -> R.string.ugc_type_v2_dance_otaku
    UgcTypeV2.DanceHiphop -> R.string.ugc_type_v2_dance_hiphop
    UgcTypeV2.DanceGestures -> R.string.ugc_type_v2_dance_gestures
    UgcTypeV2.DanceStar -> R.string.ugc_type_v2_dance_star
    UgcTypeV2.DanceChina -> R.string.ugc_type_v2_dance_china
    UgcTypeV2.DanceTutorial -> R.string.ugc_type_v2_dance_tutorial
    UgcTypeV2.DanceBallet -> R.string.ugc_type_v2_dance_ballet
    UgcTypeV2.DanceWota -> R.string.ugc_type_v2_dance_wota
    UgcTypeV2.DanceOther -> R.string.ugc_type_v2_dance_other

    UgcTypeV2.Cinephile -> R.string.ugc_type_v2_cinephile
    UgcTypeV2.CinephileCommentary -> R.string.ugc_type_v2_cinephile_commentary
    UgcTypeV2.CinephileMontage -> R.string.ugc_type_v2_cinephile_montage
    UgcTypeV2.CinephileInformation -> R.string.ugc_type_v2_cinephile_information
    UgcTypeV2.CinephilePorterage -> R.string.ugc_type_v2_cinephile_porterage
    UgcTypeV2.CinephileShortFilm -> R.string.ugc_type_v2_cinephile_shortfilm
    UgcTypeV2.CinephileAi -> R.string.ugc_type_v2_cinephile_ai
    UgcTypeV2.CinephileReaction -> R.string.ugc_type_v2_cinephile_reaction
    UgcTypeV2.CinephileOther -> R.string.ugc_type_v2_cinephile_other

    UgcTypeV2.Ent -> R.string.ugc_type_v2_ent
    UgcTypeV2.EntCommentary -> R.string.ugc_type_v2_ent_commentary
    UgcTypeV2.EntMontage -> R.string.ugc_type_v2_ent_montage
    UgcTypeV2.EntFansVideo -> R.string.ugc_type_v2_ent_fans_video
    UgcTypeV2.EntInformation -> R.string.ugc_type_v2_ent_information
    UgcTypeV2.EntReaction -> R.string.ugc_type_v2_ent_reaction
    UgcTypeV2.EntVariety -> R.string.ugc_type_v2_ent_variety
    UgcTypeV2.EntOther -> R.string.ugc_type_v2_ent_other

    UgcTypeV2.Knowledge -> R.string.ugc_type_v2_knowledge
    UgcTypeV2.KnowledgeExam -> R.string.ugc_type_v2_knowledge_exam
    UgcTypeV2.KnowledgeLangSkill -> R.string.ugc_type_v2_knowledge_lang_skill
    UgcTypeV2.KnowledgeCampus -> R.string.ugc_type_v2_knowledge_campus
    UgcTypeV2.KnowledgeBusiness -> R.string.ugc_type_v2_knowledge_business
    UgcTypeV2.KnowledgeSocialObservation -> R.string.ugc_type_v2_knowledge_social_observation
    UgcTypeV2.KnowledgePolitics -> R.string.ugc_type_v2_knowledge_politics
    UgcTypeV2.KnowledgeHumanityHistory -> R.string.ugc_type_v2_knowledge_humanity_history
    UgcTypeV2.KnowledgeDesign -> R.string.ugc_type_v2_knowledge_design
    UgcTypeV2.KnowledgePsychology -> R.string.ugc_type_v2_knowledge_psychology
    UgcTypeV2.KnowledgeCareer -> R.string.ugc_type_v2_knowledge_career
    UgcTypeV2.KnowledgeScience -> R.string.ugc_type_v2_knowledge_science
    UgcTypeV2.KnowledgeOther -> R.string.ugc_type_v2_knowledge_other

    UgcTypeV2.Tech -> R.string.ugc_type_v2_tech
    UgcTypeV2.TechComputer -> R.string.ugc_type_v2_tech_computer
    UgcTypeV2.TechPhone -> R.string.ugc_type_v2_tech_phone
    UgcTypeV2.TechPad -> R.string.ugc_type_v2_tech_pad
    UgcTypeV2.TechPhotography -> R.string.ugc_type_v2_tech_photography
    UgcTypeV2.TechMachine -> R.string.ugc_type_v2_tech_machine
    UgcTypeV2.TechCreate -> R.string.ugc_type_v2_tech_create
    UgcTypeV2.TechOther -> R.string.ugc_type_v2_tech_other

    UgcTypeV2.Information -> R.string.ugc_type_v2_information
    UgcTypeV2.InformationPolitics -> R.string.ugc_type_v2_information_politics
    UgcTypeV2.InformationOverseas -> R.string.ugc_type_v2_information_overseas
    UgcTypeV2.InformationSocial -> R.string.ugc_type_v2_information_social
    UgcTypeV2.InformationOther -> R.string.ugc_type_v2_information_other

    UgcTypeV2.Food -> R.string.ugc_type_v2_food
    UgcTypeV2.FoodMake -> R.string.ugc_type_v2_food_make
    UgcTypeV2.FoodDetective -> R.string.ugc_type_v2_food_detective
    UgcTypeV2.FoodCommentary -> R.string.ugc_type_v2_food_commentary
    UgcTypeV2.FoodRecord -> R.string.ugc_type_v2_food_record
    UgcTypeV2.FoodOther -> R.string.ugc_type_v2_food_other

    UgcTypeV2.Shortplay -> R.string.ugc_type_v2_shortplay
    UgcTypeV2.ShortplayPlot -> R.string.ugc_type_v2_shortplay_plot
    UgcTypeV2.ShortplayLang -> R.string.ugc_type_v2_shortplay_lang
    UgcTypeV2.ShortplayUpVariety -> R.string.ugc_type_v2_shortplay_up_variety
    UgcTypeV2.ShortplayInterview -> R.string.ugc_type_v2_shortplay_interview

    UgcTypeV2.Car -> R.string.ugc_type_v2_car
    UgcTypeV2.CarCommentary -> R.string.ugc_type_v2_car_commentary
    UgcTypeV2.CarCulture -> R.string.ugc_type_v2_car_culture
    UgcTypeV2.CarLife -> R.string.ugc_type_v2_car_life
    UgcTypeV2.CarTech -> R.string.ugc_type_v2_car_tech
    UgcTypeV2.CarOther -> R.string.ugc_type_v2_car_other

    UgcTypeV2.Fashion -> R.string.ugc_type_v2_fashion
    UgcTypeV2.FashionMakeup -> R.string.ugc_type_v2_fashion_makeup
    UgcTypeV2.FashionSkincare -> R.string.ugc_type_v2_fashion_skincare
    UgcTypeV2.FashionCos -> R.string.ugc_type_v2_fashion_cos
    UgcTypeV2.FashionOutfits -> R.string.ugc_type_v2_fashion_outfits
    UgcTypeV2.FashionAccessories -> R.string.ugc_type_v2_fashion_accessories
    UgcTypeV2.FashionJewelry -> R.string.ugc_type_v2_fashion_jewelry
    UgcTypeV2.FashionTrick -> R.string.ugc_type_v2_fashion_trick
    UgcTypeV2.FashionCommentary -> R.string.ugc_type_v2_fashion_commentary
    UgcTypeV2.FashionOther -> R.string.ugc_type_v2_fashion_other

    UgcTypeV2.Sports -> R.string.ugc_type_v2_sports
    UgcTypeV2.SportsTrend -> R.string.ugc_type_v2_sports_trend
    UgcTypeV2.SportsFootball -> R.string.ugc_type_v2_sports_football
    UgcTypeV2.SportsBasketball -> R.string.ugc_type_v2_sports_basketball
    UgcTypeV2.SportsRunning -> R.string.ugc_type_v2_sports_running
    UgcTypeV2.SportsKungfu -> R.string.ugc_type_v2_sports_kungfu
    UgcTypeV2.SportsFighting -> R.string.ugc_type_v2_sports_fighting
    UgcTypeV2.SportsBadminton -> R.string.ugc_type_v2_sports_badminton
    UgcTypeV2.SportsInformation -> R.string.ugc_type_v2_sports_information
    UgcTypeV2.SportsMatch -> R.string.ugc_type_v2_sports_match
    UgcTypeV2.SportsOther -> R.string.ugc_type_v2_sports_other

    UgcTypeV2.Animal -> R.string.ugc_type_v2_animal
    UgcTypeV2.AnimalCat -> R.string.ugc_type_v2_animal_cat
    UgcTypeV2.AnimalDog -> R.string.ugc_type_v2_animal_dog
    UgcTypeV2.AnimalReptiles -> R.string.ugc_type_v2_animal_reptiles
    UgcTypeV2.AnimalScience -> R.string.ugc_type_v2_animal_science
    UgcTypeV2.AnimalOther -> R.string.ugc_type_v2_animal_other

    UgcTypeV2.Vlog -> R.string.ugc_type_v2_vlog
    UgcTypeV2.VlogLife -> R.string.ugc_type_v2_vlog_life
    UgcTypeV2.VlogStudent -> R.string.ugc_type_v2_vlog_student
    UgcTypeV2.VlogCareer -> R.string.ugc_type_v2_vlog_career
    UgcTypeV2.VlogOther -> R.string.ugc_type_v2_vlog_other

    UgcTypeV2.Painting -> R.string.ugc_type_v2_painting
    UgcTypeV2.PaintingAcg -> R.string.ugc_type_v2_painting_acg
    UgcTypeV2.PaintingNoneAcg -> R.string.ugc_type_v2_painting_none_acg
    UgcTypeV2.PaintingTutorial -> R.string.ugc_type_v2_painting_tutorial
    UgcTypeV2.PaintingOther -> R.string.ugc_type_v2_painting_other

    UgcTypeV2.Ai -> R.string.ugc_type_v2_ai
    UgcTypeV2.AiTutorial -> R.string.ugc_type_v2_ai_tutorial
    UgcTypeV2.AiInformation -> R.string.ugc_type_v2_ai_information
    UgcTypeV2.AiOther -> R.string.ugc_type_v2_ai_other

    UgcTypeV2.Home -> R.string.ugc_type_v2_home
    UgcTypeV2.HomeTrade -> R.string.ugc_type_v2_home_trade
    UgcTypeV2.HomeRenovation -> R.string.ugc_type_v2_home_renovation
    UgcTypeV2.HomeFurniture -> R.string.ugc_type_v2_home_furniture
    UgcTypeV2.HomeAppliances -> R.string.ugc_type_v2_home_appliances

    UgcTypeV2.Outdoors -> R.string.ugc_type_v2_outdoors
    UgcTypeV2.OutdoorsCamping -> R.string.ugc_type_v2_outdoors_camping
    UgcTypeV2.OutdoorsHiking -> R.string.ugc_type_v2_outdoors_hiking
    UgcTypeV2.OutdoorsExplore -> R.string.ugc_type_v2_outdoors_explore
    UgcTypeV2.OutdoorsOther -> R.string.ugc_type_v2_outdoors_other

    UgcTypeV2.Gym -> R.string.ugc_type_v2_gym
    UgcTypeV2.GymScience -> R.string.ugc_type_v2_gym_science
    UgcTypeV2.GymTutorial -> R.string.ugc_type_v2_gym_tutorial
    UgcTypeV2.GymRecord -> R.string.ugc_type_v2_gym_record
    UgcTypeV2.GymFigure -> R.string.ugc_type_v2_gym_figure
    UgcTypeV2.GymOther -> R.string.ugc_type_v2_gym_other

    UgcTypeV2.Handmake -> R.string.ugc_type_v2_handmake
    UgcTypeV2.HandmakeHandbook -> R.string.ugc_type_v2_handmake_handbook
    UgcTypeV2.HandmakeLight -> R.string.ugc_type_v2_handmake_light
    UgcTypeV2.HandmakeTraditional -> R.string.ugc_type_v2_handmake_traditional
    UgcTypeV2.HandmakeRelief -> R.string.ugc_type_v2_handmake_relief
    UgcTypeV2.HandmakeDiy -> R.string.ugc_type_v2_handmake_diy
    UgcTypeV2.HandmakeOther -> R.string.ugc_type_v2_handmake_other

    UgcTypeV2.Travel -> R.string.ugc_type_v2_travel
    UgcTypeV2.TravelRecord -> R.string.ugc_type_v2_travel_record
    UgcTypeV2.TravelStrategy -> R.string.ugc_type_v2_travel_strategy
    UgcTypeV2.TravelCity -> R.string.ugc_type_v2_travel_city
    UgcTypeV2.TravelTransport -> R.string.ugc_type_v2_travel_transport

    UgcTypeV2.Rural -> R.string.ugc_type_v2_rural
    UgcTypeV2.RuralPlanting -> R.string.ugc_type_v2_rural_planting
    UgcTypeV2.RuralFishing -> R.string.ugc_type_v2_rural_fishing
    UgcTypeV2.RuralHarvest -> R.string.ugc_type_v2_rural_harvest
    UgcTypeV2.RuralTech -> R.string.ugc_type_v2_rural_tech
    UgcTypeV2.RuralLife -> R.string.ugc_type_v2_rural_life

    UgcTypeV2.Parenting -> R.string.ugc_type_v2_parenting
    UgcTypeV2.ParentingPregnantCare -> R.string.ugc_type_v2_parenting_pregnant_care
    UgcTypeV2.ParentingInfantCare -> R.string.ugc_type_v2_parenting_infant_care
    UgcTypeV2.ParentingTalent -> R.string.ugc_type_v2_parenting_talent
    UgcTypeV2.ParentingCute -> R.string.ugc_type_v2_parenting_cute
    UgcTypeV2.ParentingInteraction -> R.string.ugc_type_v2_parenting_interaction
    UgcTypeV2.ParentingEducation -> R.string.ugc_type_v2_parenting_education
    UgcTypeV2.ParentingOther -> R.string.ugc_type_v2_parenting_other

    UgcTypeV2.Health -> R.string.ugc_type_v2_health
    UgcTypeV2.HealthScience -> R.string.ugc_type_v2_health_science
    UgcTypeV2.HealthRegimen -> R.string.ugc_type_v2_health_regimen
    UgcTypeV2.HealthSexes -> R.string.ugc_type_v2_health_sexes
    UgcTypeV2.HealthPsychology -> R.string.ugc_type_v2_health_psychology
    UgcTypeV2.HealthAsmr -> R.string.ugc_type_v2_health_asmr
    UgcTypeV2.HealthOther -> R.string.ugc_type_v2_health_other

    UgcTypeV2.Emotion -> R.string.ugc_type_v2_emotion
    UgcTypeV2.EmotionFamily -> R.string.ugc_type_v2_emotion_family
    UgcTypeV2.EmotionRomantic -> R.string.ugc_type_v2_emotion_romantic
    UgcTypeV2.EmotionInterpersonal -> R.string.ugc_type_v2_emotion_interpersonal
    UgcTypeV2.EmotionGrowth -> R.string.ugc_type_v2_emotion_growth

    UgcTypeV2.LifeJoy -> R.string.ugc_type_v2_life_joy
    UgcTypeV2.LifeJoyLeisure -> R.string.ugc_type_v2_life_joy_leisure
    UgcTypeV2.LifeJoyOnSite -> R.string.ugc_type_v2_life_joy_on_site
    UgcTypeV2.LifeJoyArtisticProducts -> R.string.ugc_type_v2_life_joy_artistic_products
    UgcTypeV2.LifeJoyTrendyToys -> R.string.ugc_type_v2_life_joy_trendy_toys
    UgcTypeV2.LifeJoyOther -> R.string.ugc_type_v2_life_joy_other

    UgcTypeV2.LifeExperience -> R.string.ugc_type_v2_life_experience
    UgcTypeV2.LifeExperienceSkills -> R.string.ugc_type_v2_life_experience_skills
    UgcTypeV2.LifeExperienceProcedures -> R.string.ugc_type_v2_life_experience_procedures
    UgcTypeV2.LifeExperienceMarriage -> R.string.ugc_type_v2_life_experience_marriage

    UgcTypeV2.Mysticism -> R.string.ugc_type_v2_mysticism
    UgcTypeV2.MysticismTarot -> R.string.ugc_type_v2_mysticism_tarot
    UgcTypeV2.MysticismHoroscope -> R.string.ugc_type_v2_mysticism_horoscope
    UgcTypeV2.MysticismMetaphysics -> R.string.ugc_type_v2_mysticism_metaphysics
    UgcTypeV2.MysticismHealing -> R.string.ugc_type_v2_mysticism_healing
    UgcTypeV2.MysticismOther -> R.string.ugc_type_v2_mysticism_other
}.stringRes(context)
