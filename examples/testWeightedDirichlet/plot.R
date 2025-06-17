
library(tidyverse)
setwd("~/WorkSpace/BEASTLabs/examples/testWeightedDirichlet/")

get_weighted_mean <- function(samples, weights) {
  stopifnot(length(samples) == length(weights))  # dimension check
  
  weighted_sum_x <- sum(samples * weights)
  weight_sum <- sum(weights)
  
  if (weight_sum <= 0.0) {
    stop("weight_sum must > 0 !")
  }
  
  return (weighted_sum_x / weight_sum)
}

# Read tab-delimited file with header and skip comment lines
sim_a111 <- read_tsv("WeightedDirichletA111.log", comment = "#")
sim_a222 <- read_tsv("WeightedDirichletA222.log", comment = "#")
sim_a <- read_tsv("WeightedDirichletA127.log", comment = "#")
#sim_a <- read_tsv("WeightedDirichletA731.log", comment = "#")
mcmc_no_prior <- read_tsv("testNoPrior.log", comment = "#")
mcmc_a111 <- read_tsv("testAlpha111.log", comment = "#")
mcmc_a222 <- read_tsv("testAlpha222.log", comment = "#")
mcmc_a <- read_tsv("testAlpha127.log", comment = "#")
#mcmc_a <- read_tsv("testAlpha731.log", comment = "#")


cat("mcmc_data has ", nrow(mcmc_a111), " samples, and sim_data has ", 
    nrow(sim_a111), " samples.\n")
stopifnot(nrow(mcmc_a111)-1 == nrow(sim_a111))

# Combine and label the datasets
df_no_prior <- as_tibble(mcmc_no_prior) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "NoPrior")
df_mcmc1 <- as_tibble(mcmc_a111) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "MCMCAlpha111")
df_sim1 <- as_tibble(sim_a111) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "SimAlpha111")
df_mcmc2 <- as_tibble(mcmc_a222) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "MCMCAlpha222")
df_sim2 <- as_tibble(sim_a222) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "SimAlpha222")
df_mcmc <- as_tibble(mcmc_a) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "MCMCAlpha127")
df_sim <- as_tibble(sim_a) |> select(-Sample) |> 
  mutate(across(everything(), ~ as.numeric(as.character(.)))) |> 
  mutate(source = "SimAlpha127")

#alpha = as.numeric(c(1.0,1.0,1.0))
weights = as.numeric(c(100,200,700))
weighted_mean_sim = get_weighted_mean(df_sim1[10, 1:3], weights)
weighted_mean_sim
weighted_mean_mcmc = get_weighted_mean(df_mcmc1[10, 1:3], weights)
weighted_mean_mcmc


df_all <- bind_rows(df_mcmc1, df_sim1, df_mcmc2, df_sim2, 
                    df_mcmc, df_sim) #df_no_prior
df_all_long <- df_all |> 
  pivot_longer(cols = 1:3, names_to = "dimension", values_to = "value")


p <- ggplot(df_all_long, aes(x = value, fill = source)) +
  facet_wrap(~dimension) +
  labs(title = paste0("Test Weighted Dirichlet")) +
  theme_minimal()
p1 <- p + geom_density(alpha = 0.5) + ylab("Density")
p1
ggsave(filename = "testWeightedDirichlet1.pdf", plot = p1, 
       width = 12, height = 6)

p2 <- ggplot(df_all_long, aes(x = value, fill = source)) +
  facet_grid( source ~ dimension ) +
  geom_histogram(bins = 100, alpha = 0.5, position = "identity") + 
  labs(title = paste0("Test Weighted Dirichlet, weights = [100 200 700]")) + ylab("Frequency") +
  theme_minimal() + theme(legend.position = "none")
p2
ggsave(filename = "testWeightedDirichlet2.pdf", plot = p2, 
       width = 9, height = 12)

# Step 1: Create an alpha_id column (e.g., "Alpha1", "Alpha2", "Alpha127")
df_all_long <- df_all_long %>%
  mutate(alpha_id = str_extract(source, "Alpha\\d+"),
         alpha_id = if_else(is.na(alpha_id), source, alpha_id))

# Step 2: Plot with faceting by alpha_id and dimension
p3 <- ggplot(df_all_long, aes(x = value, fill = source)) +
  facet_grid(alpha_id ~ dimension, scale="free") +
  #geom_freqpoly(bins = 1000, size=0.3, alpha = 0.6, linetype="dotted", position = "identity") +
  #geom_histogram(bins = 100, position = "identity") +
  geom_density(alpha = 0.3, size = 0.1, position = "identity") +
  geom_vline(xintercept=1.0, linetype = "dashed", linewidth = 0.3) +
  # guides(color = guide_legend(override.aes = list(
  #   linetype = "solid",  # solid line in legend
  #   size = 10,          # thicker line in legend
  #   alpha = 1            # full opacity in legend
  # ))) +
  scale_color_brewer(palette = "Dark2") +
  scale_fill_brewer(palette = "Dark2") +
  labs( title = "Test Weighted Dirichlet, weights = [100 200 700]",
        y = "Frequency") +
  theme_minimal() +
  theme( strip.text = element_text(face = "bold"),
    plot.title = element_text(hjust = 0.5) )
p3
ggsave(filename = "testWeightedDirichlet.pdf", plot = p3, 
       width = 8, height = 6)



# https://www.blopig.com/blog/2019/06/a-brief-introduction-to-ggpairs/
library(GGally)

# p2 <- ggpairs(df_all, mapping = aes(color = source, alpha = 0.3),
#               lower = list( # shape = 1 for hollow circles
#                 continuous = wrap("smooth", shape = 1, size = .1, alpha = 0.1)
#               )) +
#   theme_minimal()
# p2
# ggsave(filename = "ggpairs.pdf", plot = p2, width = 8, height = 8)

df_sub <- df_all |> 
  group_by(source) |> 
  slice_sample(prop = 0.01) |>  # 1% of each group
  ungroup()

p2 <- ggpairs(df_sub, mapping = ggplot2::aes(color = source, alpha = 0.3),
              lower = list( # shape = 1 for hollow circles
                continuous = wrap("smooth", shape = 1, size = .1, alpha = 0.1)  
              )) +
  labs(title = paste0("Test Dirichlet : 1% samlpes")) + 
  theme_minimal()
p2
ggsave(filename = "ggpairs-1per-WeightedDirichlet.pdf", plot = p2, 
       width = 12, height = 12)



# Ensure source is a factor
df_all$source <- as.factor(df_all$source)
# Run MANOVA
#manova_test <- manova(cbind(r1, r2, r3) ~ source, data = df_all)
# Summary (Wilks’ Lambda by default)
#summary(manova_test)

manova_test_2d <- manova(cbind(r1, r2) ~ source, data = df_all)
summary(manova_test_2d)
manova_test_2d <- manova(cbind(r1, r3) ~ source, data = df_all)
summary(manova_test_2d)
manova_test_2d <- manova(cbind(r2, r3) ~ source, data = df_all)
summary(manova_test_2d)

